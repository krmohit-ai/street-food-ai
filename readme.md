# StreetFoodAI 🛒🤖

> An AI-powered Android platform that empowers street food vendors with real-time demand forecasting, optimal location scoring, and smart business analytics — while helping customers discover active vendors on a live map.

---

## 📋 Overview

**StreetFoodAI** is a full-stack AI platform for street food vendors and mobile carts in urban India. It turns guesswork into data-driven decisions — telling vendors *where* to park, *what* to stock, and *how* their business is performing.

- 🏪 **Vendors** — Log sales, track expenses, broadcast live location, and get AI-powered hotspot advice.
- 🧑‍🤝‍🧑 **Customers** — Discover nearby active vendors on an interactive map and search for dishes.

> **Status:** Hackathon MVP (~70% complete). Backend, ML pipeline, and Android UI are largely done. The Android data layer is missing and blocks compilation.

---

## ✨ Features

**Vendor**
- ✅ Google OAuth 2.0 + JWT role-based auth (vendor / customer)
- ✅ Live Open/Closed toggle with GPS broadcast to customer map
- ✅ POS billing — select items, Cash or UPI, complete sale
- ✅ Expense logging and menu management
- ✅ Weekly revenue, hourly sales chart, profit margin analytics
- ✅ XGBoost spatial demand forecasting (10×10 grid)
- ✅ Top-3 hotspot recommendations with opportunity scores
- ✅ Google Gemini LLM natural-language business advice
- 🚧 Automated model retraining (pipeline ready, scheduling not wired)
- 🚧 Real-time weather features (mock values used)

**Customer**
- ✅ Interactive OSMdroid map showing nearby active vendors
- ✅ Dish search bar (each search becomes a vendor demand signal)
- ✅ Vendor menu preview + "Request Food" demand submission
- 🚧 Reviews & ratings (endpoint stubbed)
- 🚧 Live device GPS (static coordinates in MVP)

**Backend / Platform**
- ✅ FastAPI REST API with PostgreSQL + Redis
- ✅ Redis geospatial indexing (`GEOADD` / `GEORADIUS`)
- ✅ 180-day synthetic data generator for ML training
- ✅ XGBoost training pipeline + Alembic migrations
- ✅ Docker Compose environment (PostgreSQL 15 + Redis 7)

---

## 🛠 Tech Stack

| Category | Technology |
|---|---|
| Mobile | Kotlin, Jetpack Compose, Material 3, MVVM, Hilt, StateFlow |
| Maps | OSMdroid (OpenStreetMap / CartoDB Positron) |
| Networking | Retrofit 2, OkHttp 4 |
| Auth | Google OAuth 2.0, Firebase Auth, JWT (HS256) |
| Backend | Python 3.10+, FastAPI, Uvicorn |
| ORM / DB | SQLAlchemy 2.0, Alembic, PostgreSQL 15 |
| Cache / Geo | Redis 7 (Geospatial Sorted Sets) |
| ML | XGBoost, Scikit-Learn, Pandas, NumPy |
| LLM | Google Gemini 1.5 Flash |
| DevOps | Docker Compose |

---

## 🏗 Architecture

```mermaid
flowchart TD
    A[Android Jetpack Compose UI] --> B[Hilt ViewModels]
    B --> C[Data Layer — Repositories + Retrofit]
    C -->|HTTP REST + JWT| D[FastAPI Backend]
    D --> E[Auth Router]
    D --> F[Vendor Router]
    D --> G[Customer Router]
    F --> H[PostgreSQL 15]
    F --> I[Redis 7 Geostore]
    F --> J[XGBoost + Gemini LLM]
    G --> H
    G --> I
    E --> H
```

| Layer | Responsibility |
|---|---|
| **UI** | 10 Jetpack Compose screens |
| **ViewModel** | MVVM + StateFlow state management |
| **Data Layer** ⚠️ *Missing* | Retrofit API, Repositories, TokenManager, DTOs |
| **Backend** | FastAPI routers — Auth, Vendor, Customer |
| **Database** | PostgreSQL (relational) + Redis (geospatial cache) |
| **ML / AI** | XGBRegressor demand grid + Gemini LLM advice |

---

## 📁 Project Structure

```text
street-food-ai/
├── androidApp/
│   └── src/main/java/com/example/streetfoodai/
│       ├── di/                  # Hilt DI modules (Network, Repository)
│       ├── ui/
│       │   ├── auth/            # LoginScreen, AuthViewModel
│       │   ├── vendor/          # Dashboard, POS, Expense, Menu, Recommendations
│       │   ├── customer/        # CustomerHomeScreen, CustomerViewModel
│       │   ├── components/      # AnalyticsCharts, ProfileScreen
│       │   └── navigation/      # AppNavigation, Screen, SplashViewModel
│       └── util/Constants.kt    # BASE_URL and route constants
├── backend/
│   ├── app/
│   │   ├── main.py              # App init, CORS, router registration
│   │   ├── models.py            # SQLAlchemy ORM entities
│   │   ├── schemas.py           # Pydantic DTOs
│   │   ├── auth.py              # JWT helpers, Google token verification
│   │   └── routers/             # auth.py, vendor.py, customer.py
│   ├── docker-compose.yml
│   └── requirements.txt
├── ml/
│   ├── train_forecasting_model.py
│   ├── inference.py
│   └── models/xgboost_demand.joblib
├── scripts/seed_synthetic_data.py
└── docs/                        # API_CONTRACT.md, DATABASE_SCHEMA.md, etc.
```

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio, JDK 17+
- Python 3.10+, Docker & Docker Compose
- Google Cloud project with OAuth 2.0 + Gemini API enabled
- `google-services.json` at `androidApp/app/google-services.json`

### Clone
```bash
git clone <YOUR_REPOSITORY_URL>
cd street-food-ai
```

### Backend

```bash
cd backend
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
```

Create `backend/.env`:
```env
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/streetfoodai
REDIS_URL=redis://localhost:6379
SECRET_KEY=<YOUR_STRONG_JWT_SECRET>
GEMINI_API_KEY=<YOUR_GEMINI_API_KEY>
GOOGLE_CLIENT_ID=<YOUR_GOOGLE_OAUTH_CLIENT_ID>
```

```bash
docker-compose up -d           # Start PostgreSQL + Redis
python run_migrations.py        # Run Alembic migrations
python scripts/seed_synthetic_data.py   # (Optional) seed 180-day data
python ml/train_forecasting_model.py    # (Optional) train XGBoost model
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### Android

1. Update `BASE_URL` in `Constants.kt` to your backend host.
2. Place `google-services.json` at `androidApp/app/`.
3. **Create the missing data layer** (see below), then build:

```bash
cd androidApp && ./gradlew assembleDebug
```

> ⚠️ **Missing: `com.example.streetfoodai.data`** — Create these files before building:
> ```
> data/api/StreetFoodApi.kt       # Retrofit interface
> data/api/AuthInterceptor.kt     # OkHttp Bearer token interceptor
> data/local/TokenManager.kt      # DataStore JWT manager
> data/model/Models.kt            # DTO data classes
> data/repository/AuthRepository.kt
> data/repository/VendorRepository.kt
> ```
> Match all DTOs to `docs/API_CONTRACT.md`.

---

## 🗄 Database

**PostgreSQL 15** (relational) + **Redis 7** (geospatial cache)

```mermaid
erDiagram
    users ||--|| vendor_profiles : has
    vendor_profiles ||--o{ products : lists
    vendor_profiles ||--o{ transactions : records
    vendor_profiles ||--o{ expenses : logs
    transactions ||--o{ transaction_items : contains
    products ||--o{ transaction_items : included-in
    users ||--o{ search_logs : generates
```

Redis keys: `vendor:locations` (Geo Sorted Set) and `vendor:status:<id>` (Hash cache).

---

## 🔐 Security

- Google ID Token → Backend JWT (HS256) → Bearer auth on all protected routes
- Role-based access: `/api/vendor/*` restricted to vendor role
- ⚠️ **Before production:** disable mock token bypass, set a strong `SECRET_KEY`, set `GOOGLE_CLIENT_ID` as OAuth audience, and restrict CORS origins
- Never commit `.env`, `google-services.json`, or API keys to version control

---

## 🤖 AI & ML

**XGBoost Demand Forecasting** — scores 100 spatial grid cells using `grid_x`, `grid_y`, `hour`, `day_of_week`. Opportunity score = `Forecasted Demand / (Competitors + 1)`. Top-3 hotspots returned to vendor.

**Google Gemini 1.5 Flash** — receives hotspot context (lat/lon, distance, demand, competitor count) and generates natural-language location and inventory advice. Falls back to structured data if the API call fails.

---

## 📊 Project Status

| Component | Status |
|---|---|
| Backend REST API | ✅ Complete |
| PostgreSQL Schema + Migrations | ✅ Complete |
| Redis Geostore | ✅ Complete |
| XGBoost Training Pipeline | ✅ Complete |
| Gemini LLM Advisor | ✅ Complete |
| Android UI (10 screens) | ✅ Complete |
| Android Data Layer | ❌ Missing — blocks build |
| Customer Reviews | 🚧 Stubbed |
| Live Device GPS | 🚧 Static coords |
| Security Hardening | 🚧 Partial |
| Automated Testing | 🚧 Minimal |
