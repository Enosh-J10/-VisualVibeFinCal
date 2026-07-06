# Authentication & User Identity

FinCalc provides a secure, multi-method authentication system designed for both registered and anonymous users.

## Overview
- **Methods**: Email/Password, Google Sign-In, and Guest Mode.
- **Persistence**: Managed by Firebase Authentication with local UID-scoping for data isolation.

## Key Classes
- **`LoginActivity.kt`**: Main entry point for auth. Handles Credential Manager and legacy Google Sign-In.
- **`SignupActivity.kt`**: New user registration with email verification.
- **`UserUtils.kt`**: Helper for fetching the effective UID, handling logouts, and ensuring the Firestore profile document exists.

## Startup Safety
The app performs a "Hardened Startup" in `MainActivity.kt`:
1. It attempts to fetch the current user using a safe wrapper that catches `IllegalStateException` or `Throwable`.
2. It checks for a Guest flag in local `SharedPreferences`.
3. If no valid session is found, it defaults to the `LoginActivity`.

## Guest Mode
- **Status**: Guest mode is stateless regarding the cloud.
- **Data Isolation**: All Room database entries are saved in `fincalc_database_guest.db`.
- **Restrictions**: Cloud-dependent features (Chat, Smart Travel, AI Roast, Backup) are disabled or hidden in the UI.

## Account Deletion
- **Location**: Settings screen.
- **Security**: Requires a recent login session (standard Firebase Auth requirement).
- **Cleanup**: It deletes the Firestore profile, backups, and settings before deleting the Auth account and local DB.

## Development Rules
- Never use `currentUser!!`. Always handle the null case by redirecting to Login.
- Ensure `UserUtils.getEffectiveUid(context)` is used whenever querying user data to maintain isolation.
