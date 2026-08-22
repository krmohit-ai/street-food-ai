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

### Google Sign-In (Registration & Login)
- **Endpoint**: `POST /auth/google`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "id_token": "mock_momo",
    "role": "vendor",
    "business_name": "Koramangala Momo Spot"
  }
  ```
  *(Note: For testing locally without real Firebase accounts, use "mock_momo" or "mock_chai" as the `id_token` to bypass signature verification.)*
- **Response** (`200 OK`):
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "bearer",
    "user": {
      "id": "1cdd4932-45ee-4732-82fe-65b18f6c8609",
      "email": "momo.vendor@gmail.com",
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
      "name": "Fried Momos",
      "description": "Crispy fried veg momos",
      "price": 100.00,
      "category": "snacks",
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
    "id": "789e4567-e89b-12d3-a456-426614174222",
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
      }
    ]
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "transaction_id": "b02d8471-a477-448f-8d2b-6cd4d67399bf",
    "total_amount": 160.00,
    "payment_method": "upi",
    "created_at": "2026-08-22T06:15:30.124Z"
  }
  ```

### Log Expense
- **Endpoint**: `POST /vendor/expenses`
- **Auth Required**: Yes (Vendor)
- **Request Body**:
  ```json
  {
    "amount": 1500.00,
    "description": "Vegetables, oil, and flour",
    "category": "raw_materials"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": "c88f1212-32b4-42f1-aa8e-4a6c2bb44e45",
    "amount": 1500.00,
    "description": "Vegetables, oil, and flour",
    "category": "raw_materials",
    "created_at": "2026-08-22T06:20:00.125Z"
  }
  ```

---

## 5. Location and Status Updates

### Update Location & Active Status
- **Endpoint**: `POST /vendor/location`
- **Auth Required**: Yes (Vendor)
- **Request Body**:
  ```json
  {
    "latitude": 12.9352,
    "longitude": 77.6245,
    "status": "open"
  }
  ```
  *(Note: status can be "open", "closed", or "moving". Sending "closed" takes the vendor offline and clears their record from the live Redis map geostore.)*
- **Response** (`200 OK`):
  ```json
  {
    "message": "Location and status updated successfully",
    "timestamp": "2026-08-22T06:40:12.028519Z"
  }
  ```

---

## 6. Business Intelligence & AI API

### Get AI Hotspot Recommendations
- **Endpoint**: `GET /vendor/recommendations`
- **Auth Required**: Yes (Vendor)
- **Response** (`200 OK`):
  ```json
  {
    "vendor_id": "1cdd4932-45ee-4732-82fe-65b18f6c8609",
    "recommendations": [
      {
        "grid_y": 3,
        "grid_x": 3,
        "latitude": 12.932,
        "longitude": 77.62135,
        "forecasted_demand": 2710.91,
        "active_competitors": 1,
        "score": 1355.45,
        "distance_km": 0.49
      },
      {
        "grid_y": 5,
        "grid_x": 4,
        "latitude": 12.936,
        "longitude": 77.62345,
        "forecasted_demand": 357.69,
        "active_competitors": 0,
        "score": 357.69,
        "distance_km": 0.14
      },
      {
        "grid_y": 5,
        "grid_x": 0,
        "latitude": 12.936,
        "longitude": 77.61505,
        "forecasted_demand": 73.55,
        "active_competitors": 0,
        "score": 73.55,
        "distance_km": 1.03
      }
    ],
    "ai_advice": "• High demand of Rs 2711 detected at Hotspot 1 (Lat: 12.932, Lon: 77.62135), just 0.49 km away. It has zero competitive density.\n• Prioritize stocking key ingredients for snacks as demand is projected to spike within the hour.",
    "timestamp": "2026-08-22T06:40:15.148980Z"
  }
  ```

### Get Dashboard Analytics (Graph Data)
- **Endpoint**: `GET /vendor/analytics`
- **Auth Required**: Yes (Vendor)
- **Response** (`200 OK`):
  ```json
  {
    "weekly_revenue": [
      { "date": "2026-08-15", "revenue": 20080.0 },
      { "date": "2026-08-16", "revenue": 8760.0 },
      { "date": "2026-08-17", "revenue": 10400.0 },
      { "date": "2026-08-18", "revenue": 9220.0 },
      { "date": "2026-08-19", "revenue": 8280.0 },
      { "date": "2026-08-20", "revenue": 9080.0 },
      { "date": "2026-08-21", "revenue": 15620.0 }
    ],
    "hourly_sales": [
      { "hour": 17, "count": 15, "revenue": 1420.0 },
      { "hour": 18, "count": 22, "revenue": 2040.0 }
    ],
    "category_distribution": [
      { "category": "snacks", "sales": 2153500.0 }
    ],
    "profit_margin": {
      "total_revenue": 2153500.0,
      "total_expenses": 76090.0,
      "net_profit": 2077410.0
    }
  }
  ```
