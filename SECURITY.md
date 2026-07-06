# Security & Privacy Policy (SECURITY.md)

FinCalc is built with a "Privacy-First" and "Hardened-Startup" philosophy. This document outlines the security architecture and rules for developers.

---

## 1. Authentication & Authorization
- **Model**: Uses Firebase Authentication (Email/Password and Google Sign-In).
- **Guest Mode**: Completely isolated. No data is synced to the cloud. UID is hardcoded as "guest".
- **Firestore Rules**:
    - **Backups**: Strictly owner-only read/write (`isOwner(userId)`).
    - **Smart Travel**: Only the creator (Admin) can delete a trip or update metadata. members can read and add expenses.
    - **Chat**: Read/Write restricted to UIDs present in the `memberUids` array of the parent chat document.

---

## 2. Hardened Startup
FinCalc uses a "Defense in Depth" strategy for app initialization:
- **Firebase Safety**: `FirebaseAuth.getInstance()` and related calls are wrapped in `Throwable` catches to prevent crashes if the Google Services configuration is missing or invalid.
- **Crypto Reliability**: `EncryptedSharedPreferences` has a multi-stage fallback. If the Android KeyStore is corrupted or inaccessible, the app falls back to a secondary regular `SharedPreferences` set to prevent startup crashes.
- **Database Safety**: Database initialization is scoped by UID and handles null session cases gracefully by defaulting to an "anonymous" or "guest" DB name.

---

## 3. Secret & API Management
- **local.properties**: All sensitive keys (e.g., `GEMINI_API_KEY`) must be stored here and accessed via `BuildConfig`.
- **Zero Secrets in Source**: No API keys, tokens, or private URLs should ever be hardcoded in Kotlin or XML.
- **Logging**:
    - `Log.d` and `println` should not be used in production-bound code.
    - Personal data (Emails, Names, Chat text) must **never** be printed to Logcat.

---

## 4. Input Validation & Sanitization
- **Firestore IDs**: Use `ValidationUtils.sanitizeDocId()` for any string used as a document identifier to prevent path traversal or malformed path errors.
- **Numeric Inputs**: All financial inputs must pass through `formatNumericInput` to ensure consistency and prevent `NumberFormatException`.
- **Length Limits**:
    - AI Prompts: Max 5,000 characters.
    - Chat Messages: Max 2,000 characters.
    - Note Titles: Max 100 characters.

---

## 5. Data Deletion
- **Mandatory Deletion**: In compliance with Google Play policies, users can delete their accounts from the Settings screen.
- **Process**:
    1. Scrub Firestore user profile and subcollections (`backups`, `settings`).
    2. Call `user.delete()` in Firebase Auth (handles re-auth logic).
    3. Wipe all local user data and isolated database.

---

## 6. Networking
- **HTTPS**: Cleartext traffic is disabled in `AndroidManifest.xml`.
- **Timeouts**: All Retrofit/OkHttp clients are configured with a minimum 15-30s timeout to prevent UI freezes on slow connections.

---
**Any security vulnerability identified must be addressed immediately with a stable patch before any new features are developed.**
