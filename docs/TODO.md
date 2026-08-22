# StreetFoodAI - Developer Task Board

This Kanban board tracks development across backend, ML, and frontend components.

---

## 📋 BACKLOG

### Backend & ML
- [ ] Implement WebSocket connection for real-time location updates (Stretch goal).
- [ ] Implement automated cache invalidation on menu updates.
- [ ] Connect live weather fetching from OpenWeatherMap API (rather than mock weather).

### Frontend (Flutter)
- [ ] Add customer rating submission.
- [ ] Implement cart navigation routing using OSMap directions.
- [ ] Save login token locally using `flutter_secure_storage`.

---

## 📅 TO DO

### Phase 2: Base Infrastructure
- [ ] Create `docker-compose.yml` for PostgreSQL/Redis (Owner: Member 1).
- [ ] Configure Alembic and run initial DB migrations (Owner: Member 1).
- [ ] Build Flutter base skeletons and routing (Owner: Member 2).
- [ ] Setup Flutter mock API client with hardcoded models (Owner: Member 2).

### Phase 3: Core CRUD & Data Seeding
- [ ] Implement user Auth endpoints in FastAPI (Owner: Member 1).
- [ ] Create Menu Management CRUD APIs (Owner: Member 1).
- [ ] Create POS Billing and Expense Logging APIs (Owner: Member 1).
- [ ] Write `seed_synthetic_data.py` database generation script (Owner: Member 1).
- [ ] Build POS Billing and Expense layouts in Flutter (Owner: Member 2).
- [ ] Build Active Vendor Map Screen in Flutter (Owner: Member 2).

### Phase 4: AI & Machine Learning
- [ ] Implement XGBoost offline training pipeline (Owner: Member 1).
- [ ] Code Location Scoring and Inventory Optimizer functions (Owner: Member 1).
- [ ] Wire Gemini LLM response formatting (Owner: Member 1).
- [ ] Integrate charts into Vendor Dashboard (Owner: Member 2).
- [ ] Design Recommendation View screen (Owner: Member 2).

---

## 🔄 IN PROGRESS

- [ ] Write shared documentation contracts (`docs/`) (Owner: Member 1 & 2).

---

## ✅ DONE

- [x] Define product vision and hackathon MVP scope.
- [x] Formulate product architecture and tech stack decisions.
- [x] Map out database relationships and hybrid cache structure.
- [x] Define REST API endpoints and mock JSON formats.
- [x] Formulate demand forecasting, location ranking, and inventory optimization math.
