# Implementation Plan: StreetFood AI - Foundation & Auth

This plan covers the initial setup of the Android application, including dependency injection, networking, and the authentication flow.

## User Review Required

> [!IMPORTANT]
> **Google Maps API Key:** I will use a placeholder `MAPS_API_KEY` in `AndroidManifest.xml`. You will need to replace this with your actual key in the `local.properties` or directly in the manifest.
> **Signing Fingerprints:** Since I cannot access your local keystore directly, please run `./gradlew signingReport` in your terminal and add the SHA-1/SHA-256 to your Firebase Console to get the `google-services.json`.

## Proposed Changes

### 1. Build Configuration & Dependencies
Modify `libs.versions.toml` and `build.gradle.kts` to include:
- **Hilt:** For dependency injection.
- **Retrofit & OkHttp:** For API communication.
- **Navigation Compose:** For screen transitions.
- **DataStore:** For persistent storage of the JWT token.
- **Google Maps Compose:** For location features.
- **Logging Interceptor:** For debugging network requests.

### 2. Project Architecture & Folders [NEW]
Establish the directory structure:
- `data/`: `api/`, `model/`, `repository/`, `local/`
- `di/`: Hilt modules.
- `ui/`: `auth/`, `vendor/`, `customer/`, `components/`, `navigation/`
- `util/`: Constants and Helpers.

### 3. Networking Layer [NEW]
- `StreetFoodApi`: Retrofit interface defining the endpoints from `API_CONTRACT.md`.
- `AuthInterceptor`: To automatically attach the Bearer token to requests.
- `TokenManager`: Using DataStore to save/retrieve the JWT.

### 4. Authentication Flow [NEW]
- `AuthViewModel`: Handles login/registration logic.
- `LoginScreen` & `RegisterScreen`: Material 3 Compose UIs.
- `AppNavigation`: Role-based routing (Vendor vs. Customer).

## Verification Plan

### Automated Tests
- Unit tests for `AuthViewModel` using Mockito/Turbine to verify state transitions.
- Test `AuthInterceptor` to ensure headers are correctly added.

### Manual Verification
1. Launch app -> Redirect to Login.
2. Enter mock credentials (e.g., `mock_momo`) -> Verify redirection to Vendor Dashboard.
3. Verify that the JWT token is persisted across app restarts.
