# Codebase Organization (CODEBASE.md)

This document provides a technical map of the FinCalc Android project to assist developers in navigating the architecture and logic flows.

---

## 1. Project Structure
The project is a single-module Android application located in the `app` directory.

### Core Packages (`com.enosh.fincalc`)
| Package | Description |
| :--- | :--- |
| `ui.screens` | Composable screens for all features (Chat, Travel, Calculators, etc.). |
| `ui.components` | Reusable UI widgets (BouncyButton, ValidatedTextField, AssistantRobot). |
| `ui.navigation` | `NavGraph.kt` and `Screen.kt` defining the navigation hierarchy. |
| `viewmodel` | Logic and state management for every feature. |
| `data.local` | Room database (`AppDatabase`), DAOs, and Entities. |
| `data.api` | Retrofit interfaces and factories (Gemini, Currency). |
| `data.repository`| Repository implementations for abstraction. |
| `utils` | Shared utilities (Security, Validation, User, QR, Backup). |
| `worker` | Background tasks using `WorkManager` (Fun Reminders). |

---

## 2. Key Logic Flows

### Startup Flow
1. **`MainActivity`**: Displays splash animation and tips.
2. **Hardening**: Uses `Throwable` wrappers to safely attempt `FirebaseAuth` and `EncryptedSharedPreferences` access.
3. **Routing**: 
    - If user is logged in/Guest and `keep_me_signed_in` is true: Redirect to `HomeActivity` (or `LockActivity` if PIN is enabled).
    - Else: Redirect to `LoginActivity`.

### Login & Auth Flow
1. **`LoginActivity`**: Supports Email/Password, Google Sign-In, and Guest Mode.
2. **Profile Sync**: Upon successful auth, `UserUtils.ensureFinCalcUserProfile` ensures Firestore is updated with latest UID data.
3. **Account Deletion**: Located in `SettingsScreen`. Calls `UserUtils.deleteAccount` to scrub Firestore user data before closing the Auth session.

### Navigation Hierarchy
- **`NavGraph.kt`**: Uses `NavHost` to manage all Compose transitions.
- **Onboarding**: Checked via `UserUtils.getScopedKey(uid, "onboarding_complete")`.

---

## 3. Important Systems

### AI Assistant System
- **Implementation**: Uses Google Gemini Pro via Retrofit.
- **Init**: Initialized **lazily** in `AiViewModelFactory`.
- **Persona**: Controlled by `isRoastMode` in `AssistantPrefs`.

### Smart Travel System
- **Backend**: Real-time Firestore sync.
- **Hierarchy**: `trips` (root) -> `expenses` (sub) -> `flags` (sub).
- **Admin**: The `createdByUid` has exclusive delete/edit rights for the trip metadata.

### Smart Scan System
- **Bitmap Handling**: Located in `SmartScanScreen.kt`. Uses a two-pass `BitmapFactory` decode to downsample images to **2048px** max, preventing OOM.
- **OCR**: Uses Google ML Kit Text Recognition.

### Backup System
- **Cloud**: Serializes Room data + Prefs into a JSON string and stores it in `users/{uid}/backups/latest`.
- **Local**: Uses the System File Picker to save/load JSON files.

---

## 4. Android 15 & Modern Standards
- **Edge-to-Edge**: Supported globally via `enableEdgeToEdge()` in all Activities.
- **Insets**: Compose screens use `safeDrawingPadding()` or `systemBarsPadding()` via `CalculatorScreenScaffold`.
- **Scoped Storage**: Uses Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) for avatars and receipts, removing the need for broad storage permissions.

---
**Developers should maintain the MVVM pattern and avoid adding business logic directly into Composable functions.**
