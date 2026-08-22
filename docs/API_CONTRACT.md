# API Contract Specification

This document defines the REST API contract between the Flutter client and the FastAPI backend.

---

## 1. Global Specifications

### Base URL
- Local Dev: `http://localhost:8000/api`
- Staging/Production: `https://api.streetfoodai.in/api` (Placeholder)

### Headers
- Public endpoints: None
- Private endpoints: `Authorization: Bearer <JWT_TOKEN>`

### Error Format
All errors return standard HTTP status codes accompanied by this JSON structure:
```json
{
  "detail": "Error message explanation"
}
```

---

## 2. Authentication API

### Register Account
- **Endpoint**: `POST /auth/register`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "phone": "9876543210",
    "password": "Password123",
    "role": "vendor",
    "business_name": "Koramangala Momo Spot"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "phone": "9876543210",
    "role": "vendor",
    "business_name": "Koramangala Momo Spot",
    "created_at": "2026-08-22T10:00:00Z"
  }
  ```

### Login Account
- **Endpoint**: `POST /auth/login`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "phone": "9876543210",
    "password": "Password123"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "bearer",
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "phone": "9876543210",
      "role": "vendor"
    }
  }
  ```

---

## 3. Vendor Menu API

### Get Menu Items
- **Endpoint**: `GET /vendor/menu`
- **Auth Required**: Yes (Vendor)
- **Response** (`200 OK`):
  ```json
  [
    {
      "id": "789e4567-e89b-12d3-a456-426614174111",
      "name": "Steam Momos",
      "description": "Hot veg steam momos (8 pcs)",
      "price": 80.00,
      "category": "snacks",
      "is_available": true
    },
    {
      "id": "789e4567-e89b-12d3-a456-426614174222",
      "name": "Masala Chai",
      "description": "Indian ginger tea",
      "price": 15.00,
      "category": "beverages",
      "is_available": true
    }
  ]
  ```

### Add Menu Item
- **Endpoint**: `POST /vendor/menu`
- **Auth Required**: Yes (Vendor)
- **Request Body**:
  ```json
  {
    "name": "Fried Momos",
    "description": "Crispy fried veg momos",
    "price": 100.00,
    "category": "snacks"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": "789e4567-e89b-12d3-a456-426614174333",
    "name": "Fried Momos",
    "description": "Crispy fried veg momos",
    "price": 100.00,
    "category": "snacks",
    "is_available": true
  }
  ```

---

## 4. Billing & Expenses API (POS)

### Record Transaction (Sale)
- **Endpoint**: `POST /vendor/transactions`
- **Auth Required**: Yes (Vendor)
- **Request Body**:
  ```json
  {
    "payment_method": "upi",
    "items": [
      {
        "product_id": "789e4567-e89b-12d3-a456-426614174111",
        "quantity": 2
      },
      {
        "product_id": "789e4567-e89b-12d3-a456-426614174222",
        "quantity": 1
      }
    ]
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "transaction_id": "999e4567-e89b-12d3-a456-426614174999",
    "total_amount": 175.00,
    "payment_method": "upi",
    "created_at": "2026-08-22T10:45:00Z"
  }
  ```

### Log Expense
- **Endpoint**: `POST /vendor/expenses`
- **Auth Required**: Yes (Vendor)
- **Request Body**:
  ```json
  {
    "amount": 450.00,
    "description": "Cabbage, Flour, and Spices",
    "category": "raw_materials"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": "666e4567-e89b-12d3-a456-426614174666",
    "amount": 450.00,
    "description": "Cabbage, Flour, and Spices",
    "category": "raw_materials",
    "created_at": "2026-08-22T08:00:00Z"
  }
  ```

---

## 5. Location Tracking & Maps API

### Update Vendor Location
- **Endpoint**: `POST /vendor/location`
- **Auth Required**: Yes (Vendor)
- **Request Body**:
  ```json
  {
    "latitude": 12.93524,
    "longitude": 77.62453,
    "status": "open"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "message": "Location updated successfully",
    "timestamp": "2026-08-22T10:50:00Z"
  }
  ```

### Discover Nearby Vendors
- **Endpoint**: `GET /customer/vendors`
- **Auth Required**: No
- **Query Parameters**:
  - `latitude`: `12.9348` (Customer location)
  - `longitude`: `77.6240`
  - `radius`: `1.5` (km, optional, default: 2.0)
- **Response** (`200 OK`):
  ```json
  [
    {
      "vendor_id": "123e4567-e89b-12d3-a456-426614174000",
      "business_name": "Koramangala Momo Spot",
      "status": "open",
      "latitude": 12.93524,
      "longitude": 77.62453,
      "rating": 4.8,
      "distance_km": 0.07
    }
  ]
  ```

### Customer Search Food
- **Endpoint**: `GET /customer/search`
- **Auth Required**: No
- **Query Parameters**:
  - `query`: `momos`
  - `latitude`: `12.9348`
  - `longitude`: `77.6240`
- **Response** (`200 OK`):
  - *Note*: Registers search telemetry in `search_logs` and returns matches.
  ```json
  [
    {
      "vendor_id": "123e4567-e89b-12d3-a456-426614174000",
      "business_name": "Koramangala Momo Spot",
      "status": "open",
      "latitude": 12.93524,
      "longitude": 77.62453,
      "rating": 4.8
    }
  ]
  ```

---

## 6. Business Intelligence & AI API

### Get AI Recommendations
- **Endpoint**: `GET /vendor/recommendations`
- **Auth Required**: Yes (Vendor)
- **Response** (`200 OK`):
  ```json
  {
    "timestamp": "2026-08-22T10:50:00Z",
    "location_recommendations": [
      {
        "zone_name": "College Road Grid 4",
        "latitude": 12.9388,
        "longitude": 77.6292,
        "opportunity_score": 92,
        "active_competitors": 1,
        "distance_km": 0.6
      },
      {
        "zone_name": "Main Market Corner",
        "latitude": 12.9312,
        "longitude": 77.6210,
        "opportunity_score": 74,
        "active_competitors": 4,
        "distance_km": 0.8
      }
    ],
    "inventory_recommendations": [
      {
        "product_name": "Steam Momos",
        "predicted_demand_units": 130,
        "current_raw_material_prep_suggested": "130 portions",
        "confidence": "high"
      },
      {
        "product_name": "Masala Chai",
        "predicted_demand_units": 150,
        "current_raw_material_prep_suggested": "150 portions",
        "confidence": "medium"
      }
    ],
    "ai_explanation": "Demand at College Road Grid 4 is surging due to students leaving campus (5 PM peak). Nearby competition is low (only 1 other cart active). Preparing 130 plates of momos is optimal to capture maximum sales without leaving waste."
  }
  ```

### Get Dashboard Analytics
- **Endpoint**: `GET /vendor/analytics`
- **Auth Required**: Yes (Vendor)
- **Query Parameters**:
  - `range`: `weekly` (options: `daily`, `weekly`, `monthly`)
- **Response** (`200 OK`):
  ```json
  {
    "summary": {
      "total_revenue": 14200.00,
      "total_expenses": 5300.00,
      "net_profit": 8900.00,
      "profit_margin": 62.7
    },
    "sales_by_item": [
      { "item_name": "Steam Momos", "quantity": 140, "revenue": 11200.00 },
      { "item_name": "Masala Chai", "quantity": 200, "revenue": 3000.00 }
    ],
    "chart_data": [
      { "date": "2026-08-16", "revenue": 1800.00, "expenses": 600.00, "profit": 1200.00 },
      { "date": "2026-08-17", "revenue": 2100.00, "expenses": 800.00, "profit": 1300.00 },
      { "date": "2026-08-18", "revenue": 1900.00, "expenses": 700.00, "profit": 1200.00 },
      { "date": "2026-08-19", "revenue": 2400.00, "expenses": 950.00, "profit": 1450.00 },
      { "date": "2026-08-20", "revenue": 2600.00, "expenses": 1100.00, "profit": 1500.00 },
      { "date": "2026-08-21", "revenue": 3400.00, "expenses": 1150.00, "profit": 2250.00 }
    ]
  }
  ```
