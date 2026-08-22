# Project: AI for Street Food Vendors (StreetFoodAI)

## 1. Vision & Purpose
StreetFoodAI is an AI-powered real-time demand and business intelligence platform designed specifically for small street-food vendors and movable street-food carts in India. It empowers micro-entrepreneurs by replacing intuition-based business decisions with data-driven predictions, helping them answer critical questions:
- *Where should I operate right now?*
- *When should I move my cart?*
- *What food is likely to be in demand today?*
- *How much raw material should I prepare to minimize waste and maximize profit?*
- *How much profit/loss am I actually making?*

---

## 2. Core MVP Scope
To succeed in a fast-paced hackathon, the MVP is strictly limited to the core loop of demand sensing, prediction, navigation, billing, and feedback.

### Included Features (MVP)
1. **Customer Discoverability**: Interactive map showing active vendors, open/closed status, and menu details.
2. **Anonymous Demand Capture**: Log customer search queries, locations, and time as indicators of demand hotspots.
3. **ML Demand Forecaster**: Traditional regression model (XGBoost) predicting portion sales based on time, weather, and historical data.
4. **Opportunity Location Engine**: Geospatial scoring algorithm that ranks optimal operational zones.
5. **Inventory Optimization**: Recommends prep quantities balancing potential sales against waste costs.
6. **LLM Explainer**: Translates numerical forecasts and scoring metrics into simple, conversational local-business advice.
7. **Vendor Billing & POS**: Simplest cash billing interface to record transactions (sales price, quantity, timestamp).
8. **Vendor Expense Logger**: Log raw material costs to calculate accurate net profits.
9. **Analytics Dashboard**: Simple visual graphs of revenue, expenses, and net profit.

### Excluded Features (Out of Scope for MVP)
- Online ordering/pre-payments (handled as mock checkout or omitted entirely).
- Advanced routing navigation (Google Maps APIs routes - we will show straight-line distances/pins instead).
- OTP/SMS login validation (JWT credentials only).
- Customer review sentiment analysis (ratings only).

---

## 3. Core Demo Scenario
Our hackathon pitch will revolve around a single, cohesive scenario demonstrating the end-to-end feedback loop:

### The Friday Night Momo Opportunity
1. **5:00 PM**: Multiple users in the **Koramangala College Area** open the Customer App and search for "Momos".
2. **Demand Sensing**: The backend registers these searches as a spatial hotspot of unsatisfied demand (as momo vendors are currently operating elsewhere, e.g. near the Bus Stand).
3. **Recommendation Generation**: 
   - The ML Model predicts high Friday evening momo demand based on historical patterns and favorable weather (cloudy evening).
   - The Location Engine ranks the **College Area** at `92/100` and the **Bus Stand** at `64/100`.
   - The LLM summarizes: *"Move to College Area around 5:45 PM. Expected demand is high. Prepare 130 portions of Momos."*
4. **Vendor Action**: The vendor views this on the Vendor App, prepares the recommended inventory, and updates their status to "Moving to College Area".
5. **Transaction Recording**: The vendor arrives, operates, and records 125 momo sales via the billing POS.
6. **Closing the Loop**: The transactions are stored. The Vendor Dashboard updates instantly showing high profit, and the ML model ingests this new transaction to refine next week's forecasts.

---

## 4. Shared Responsibility & Ownership

| Document | File Path | Primary Owner | Secondary Reviewer | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Project Overview** | `docs/PROJECT.md` | Joint (Shared) | Joint (Shared) | **Drafted** |
| **User Flow Spec** | `docs/USER_FLOWS.md` | Member 2 (Frontend) | Member 1 (Backend) | *Pending* |
| **Database Schema** | `docs/DATABASE_SCHEMA.md` | Member 1 (Backend) | Member 2 (Frontend) | *In Progress* |
| **API Contract** | `docs/API_CONTRACT.md` | Member 1 (Backend) | Member 2 (Frontend) | *In Progress* |
| **AI Specification** | `docs/AI_SPECIFICATION.md` | Member 1 (Backend) | Member 2 (Frontend) | *In Progress* |
| **Integration Guide** | `docs/INTEGRATION.md` | Member 1 (Backend) | Member 2 (Frontend) | *Pending* |
| **Interactive Todo** | `docs/TODO.md` | Joint (Shared) | Joint (Shared) | *Pending* |
