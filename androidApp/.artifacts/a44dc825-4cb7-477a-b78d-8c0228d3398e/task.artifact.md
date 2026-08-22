# Tasks: StreetFood AI Foundation

- [ ] **Phase 1: Dependency Setup**
    - [ ] Update `libs.versions.toml` with Hilt, Retrofit, Navigation, DataStore, and Maps.
    - [ ] Configure `build.gradle.kts` (Project level) for Hilt plugin.
    - [ ] Configure `build.gradle.kts` (App level) with dependencies and Hilt setup.
    - [ ] Create `StreetFoodApplication` class and annotate with `@HiltAndroidApp`.

- [ ] **Phase 2: Project Structure**
    - [ ] Create base packages (`data`, `di`, `ui`, `util`).
    - [ ] Define API Constants (Base URL, Endpoints).

- [ ] **Phase 3: Networking & Storage**
    - [ ] Implement `TokenManager` using DataStore.
    - [ ] Implement `AuthInterceptor` for Retrofit.
    - [ ] Define `StreetFoodApi` interface and DTOs for Auth.
    - [ ] Setup Hilt `NetworkModule`.

- [ ] **Phase 4: Auth UI & Navigation**
    - [ ] Implement `LoginScreen` and `RegisterScreen`.
    - [ ] Setup `NavHost` with role-based logic.
    - [ ] Implement `AuthViewModel`.
