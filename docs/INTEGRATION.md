# Integration & Local Development Guide

This document describes how to set up the local development environment for the StreetFoodAI backend and how to connect the Flutter client for parallel testing.

---

## 1. Prerequisites

Ensure you have the following installed on your machine:
- **Docker & Docker Compose**: For launching PostgreSQL and Redis.
- **Python 3.10+**: For the FastAPI backend and ML pipeline.
- **Flutter SDK**: For running the client applications.

---

## 2. Docker & Database Setup

We use Docker to spin up PostgreSQL and Redis.

### Create `docker-compose.yml`
Create this file in the `backend/` directory:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: streetfood_db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password123
      POSTGRES_DB: streetfood_ai
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: streetfood_redis
    ports:
      - "6379:6379"
    volumes:
      - redisdata:/data

volumes:
  pgdata:
  redisdata:
```

### Launch Infrastructure
Navigate to the backend folder and run:
```bash
docker compose up -d
```

---

## 3. Backend Setup

### Environment Configuration
Create a `.env` file in the `backend/` directory:
```env
DATABASE_URL=postgresql://postgres:password123@localhost:5432/streetfood_ai
REDIS_URL=redis://localhost:6379/0
SECRET_KEY=supersecretjwtkey12345678901234567890
GEMINI_API_KEY=your_gemini_api_key_here
```

### Python Virtual Environment & Dependencies
1. Create virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate
   ```
2. Create `requirements.txt`:
   ```text
   fastapi>=0.100.0
   uvicorn[standard]>=0.22.0
   sqlalchemy>=2.0.0
   alembic>=1.11.0
   psycopg2-binary>=2.9.0
   redis>=4.6.0
   pydantic-settings>=2.0.0
   python-jose[cryptography]>=3.3.0
   passlib[bcrypt]>=1.7.4
   pandas>=2.0.0
   numpy>=1.24.0
   xgboost>=1.7.0
   joblib>=1.3.0
   google-generativeai>=0.1.0
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

### Database Migrations
Initialize and run migrations using Alembic:
```bash
alembic init alembic
alembic revision --autogenerate -m "initial schema"
alembic upgrade head
```

### Running Server
Start the local FastAPI instance:
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
Access the auto-generated Swagger UI at `http://localhost:8000/docs` to test APIs.

---

## 4. Seeding Synthetic Data

To demonstrate demand intelligence with realistic historical charts and forecast patterns, run the synthetic seeding script:
```bash
python scripts/seed_synthetic_data.py
```
This inserts:
- 3 test vendors (Momo, Samosa/Chai, Maggi).
- 6 months of historical transactions matching seasonal/time-of-day demands.
- 500 search queries focused around the Koramangala demo area to simulate unmet customer demand.

---

## 5. Connecting Flutter Client

In the Flutter code, configure the API client base URL configuration.

### Client API Environment Config (e.g. `lib/core/constants.dart`)
```dart
class APIConstants {
  // Toggle this flag to test with live backend vs mock data
  static const bool useMockData = false;

  // Local IP address of your backend development machine (use 10.0.2.2 for Android emulator)
  static const String localBaseUrl = "http://10.0.2.2:8000/api";
  
  static const String productionBaseUrl = "https://api.streetfoodai.in/api";

  static String get baseUrl => useMockData ? "" : localBaseUrl;
}
```

### Map Asset Requirements
The Flutter developer will use open-source maps:
- Package: `flutter_map` (Leaflet wrapper).
- Tile Provider: OpenStreetMap (free, no API key needed).
- Location provider: `geolocator` package.
