# AI & Machine Learning Specification

This document details the feature engineering, ML model formulation, location opportunity scoring, inventory recommendation optimization, and LLM text generation logic.

---

## 1. Demand Prediction Formulation (XGBoost)

We treat demand forecasting as a tabular regression problem. For a given **Vendor Profile**, **Product**, **Location Grid Cell**, and **Time Block**, we predict the **Expected Portions Sold**.

### Grid Cell Definition
To avoid complex maps API routes, we divide our 2km demo bounding box into a 10x10 coordinate grid of **100 cells**. Each cell is identified by a unique `grid_id` (e.g., `grid_34`).

### Feature Store Design

| Feature Name | Data Type | Description | Source |
| :--- | :--- | :--- | :--- |
| `grid_id` | Categorical (Int) | Target location zone | Database |
| `product_id` | Categorical (Int) | Target menu item | Database |
| `day_of_week` | Integer (0-6) | Monday = 0, Sunday = 6 | System Time |
| `hour_block` | Integer (0-23) | Hour of the day | System Time |
| `weather_temp` | Float | Temperature in Celsius | Weather API / Mock |
| `weather_condition`| Categorical (Str) | `sunny` \| `cloudy` \| `rainy` | Weather API / Mock |
| `recent_search_volume`| Integer | Customer searches for this item in this grid cell in the last 1 hour. | `search_logs` |
| `hist_average_sales` | Float | 4-week moving average of sales for this product, in this grid cell, on this day-of-week, at this hour. | `transactions` |

### Model Architecture
- **Algorithm**: `XGBRegressor` (Scikit-Learn/XGBoost library).
- **Target Variable**: `sold_quantity` (Integer).
- **Evaluation Metric**: Root Mean Squared Error (RMSE) & Mean Absolute Error (MAE).
- **Baseline**: Historical average sales.

---

## 2. Location Opportunity Scoring Engine

The scoring engine ranks each grid cell $c$ for a vendor based on a balance of predicted demand, unmet search interest, and local competition.

### Formula
$$Score_c = \text{Normalize}\Big( w_1 \cdot D_{c,p} + w_2 \cdot U_{c,p} - w_3 \cdot S_{c} \Big)$$

Where:
- $D_{c,p}$: Predicted demand for vendor products in cell $c$ (derived from the XGBoost model).
- $U_{c,p}$: Unmet demand search index (volume of customer searches in cell $c$ for vendor's products in the last 2 hours).
- $S_c$: Supply penalty (number of active competitors of the same category currently located in or moving to cell $c$).
- $w_1, w_2, w_3$: Weights configured in backend config (Default: $w_1 = 0.5, w_2 = 0.4, w_3 = 0.3$).

The resulting values are min-max normalized to produce an integer score between **0 and 100**.

---

## 3. Inventory Recommendation Optimization

Instead of just recommending the predicted demand, the system optimizes prep quantities based on a safety stock model that accounts for waste costs vs. missed sales margins.

### Formulation
$$\text{Prep Suggested} = \text{Demand}_{\text{pred}} + Z \cdot \sigma$$

Where:
- $\text{Demand}_{\text{pred}}$: Predicted demand (units) from the XGBoost model.
- $\sigma$: Standard deviation of historical sales for this time block.
- $Z$: Service factor (calculated using a newsvendor optimization ratio):
  $$Z = \Phi^{-1}\left( \frac{\text{Margin}}{\text{Margin} + \text{Cost of Waste}} \right)$$
  - *Example*: Masala Chai has a high profit margin (price ₹15, cost ₹3). The cost of waste is low, so the system recommends over-preparing ($Z$ is high).
  - *Example*: Paneer Momo has a lower margin (price ₹100, cost ₹60) and spoils fast. The system suggests a safer, closer-to-mean quantity.

---

## 4. LLM Advice Explainer (Gemini API)

We pass the structured results of the recommendation engine into the Gemini API to produce an easily understandable explanation for the vendor.

### LLM Prompt Template
```text
System Prompt:
You are an expert local business consultant assisting small street-food vendors in India. 
Your tone must be highly encouraging, practical, and clear. 
Translate complex analytics into simple, conversational instructions.
Use local terminology where appropriate. Keep the output under 3 sentences.

User Prompt Context:
Vendor Business Name: {business_name}
Target Move-To Location: {location_name}
Top Recommended Spot Score: {opportunity_score}/100
Competitors in area: {competitor_count}
Top demand item: {item_name} (Recommend preparing {item_prep_qty} portions)
Recent customer search activity: {recent_searches_detail}

Format the response as a direct recommendation containing:
1. Why they should move (mention demand and competitor density).
2. What inventory to prepare.
3. A tip about local events or time patterns.
```

### Example Input
- `business_name`: "Koramangala Momo Spot"
- `location_name`: "College Road Grid 4"
- `opportunity_score`: 92
- `competitor_count`: 1
- `item_name`: "Steam Momos"
- `item_prep_qty`: 130
- `recent_searches_detail`: "14 searches for momos near college road in the last hour"

### Example Output (JSON field `ai_explanation`)
> "Demand at College Road is surging due to college dismissal (5 PM peak). Nearby competition is low (only 1 other cart active). Preparing 130 plates of steam momos is optimal to capture maximum sales without leaving waste."
