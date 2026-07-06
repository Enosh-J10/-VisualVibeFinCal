# FinCalc Engineering Handbook (BRAIN.md)

This document serves as the persistent architectural reference, engineering guide, and source of truth for the FinCalc project. All developers and AI agents must review this document before proposing or implementing changes.

---

## 1. Project Overview
- **Project Name:** FinCalc (Visual Vibe FinCal)
- **Purpose:** A high-performance, premium personal finance ecosystem featuring budgeting, AI assistance, shared travel expenses (Smart Travel), business tracking (Smart Business), and productivity tools.
- **Platform:** Android (Min SDK 24, Target SDK 35).
- **Core Stack:**
    - **Language:** Kotlin
    - **UI Framework:** Jetpack Compose (100%)
    - **Design System:** Material 3 (Customized)
    - **Local Database:** Room
    - **Backend:** Firebase (Auth, Firestore, Messaging)
    - **AI:** Google Gemini (Generative AI)
- **Status:** Production / Google Play Release.

---

## 2. Architecture & Patterns
- **Pattern:** MVVM (Model-View-ViewModel).
- **UI:** Stateless Composables where possible. Theme support (Light/Dark) using `FinCalcTheme`.
- **Navigation:** Type-safe Navigation Compose with `Screen` sealed class.
- **Dependency Management:** Manual injection / ViewModel Factories. No heavy DI frameworks (Hilt/Koin) are currently implemented.
- **Concurrency:** Kotlin Coroutines & Flow.
- **Security:** `EncryptedSharedPreferences` for sensitive data (PINs, credentials) with a regular `SharedPreferences` fallback for stability.
- **Database Strategy:**
    - Uses Room with `fallbackToDestructiveMigration(true)` (Caution: Clears data on schema change).
    - Database files are scoped by User UID (`fincalc_database_$uid.db`) to ensure data isolation between accounts.
    - Guest Mode uses `fincalc_database_guest.db`.

---

## 3. Project Principles (The "Never" Rules)
1. **Stability First:** Production stability and crash prevention take priority over new features.
2. **Backwards Compatibility:** Never break existing user data structures (Room/Firestore) without a migration plan.
3. **No Unrequested Redesigns:** Do not change the look and feel of working screens unless explicitly asked.
4. **Minimal Refactoring:** Avoid refactoring stable code purely for "style" or "cleanliness" if it risks regressions.
5. **Backwards Safety:** Always use `try-catch` or `Throwable` wrappers around startup initializers (Firebase, Crypto, DB).
6. **Feature Preservation:** Never remove an existing feature during a bug fix.

---

## 4. UI/UX Guidelines
- **Theme:** Default to a "Premium Dark" aesthetic.
- **Colors:** Primary Accent is **Teal (#00D1B2)**.
- **Components:**
    - Use `CalculatorScreenScaffold` for tool screens.
    - Rounded corners (typically 12dp to 28dp) for cards and surfaces.
    - Minimal, snappy animations using `AnimatedVisibility` and `animate*AsState`.
- **Edge-to-Edge:** Support Android 15+ edge-to-edge by default. Use `safeDrawingPadding()` or `fitsSystemWindows`.

---

## 5. Firebase Structure (Firestore)

| Collection | Subcollection | Purpose |
| :--- | :--- | :--- |
| `users` | - | Core user profiles (UID, name, email, finCalcId). |
| | `backups` | Private. Stores JSON snapshots of app data for cloud restore. |
| | `friendSettings` | User-specific settings for friends (e.g., nicknames). |
| | `blockedUsers` | List of blocked UIDs. |
| `friends` | - | Friendship records using sorted deterministic IDs (`uid1_uid2`). |
| `friendRequests`| - | Pending, accepted, or rejected request status. |
| `chats` | - | Metadata for 1-on-1 chat rooms. |
| | `messages` | Real-time chat history. |
| | `status` | Presence data (Online/Typing status). |
| `notifications` | `items` | Triggers for FCM/Cloud Functions. |
| `trips` | - | Shared "Smart Travel" trip records. |
| | `expenses` | Shared expenses within a specific trip. |
| | | `flags` - Member-raised disputes/flags on expenses. |

---

## 6. Smart Travel Logic
- **Ownership:** The creator is the Admin.
- **Membership:** Admin invites via FinCalc ID; users must "Join" to see expenses.
- **Expenses:** Any joined member can add/edit expenses.
- **Flagging:** Any member can flag an expense for review (e.g., "Wrong amount").
- **Settlement:** Uses a "Minimal Transactions" algorithm to simplify debt.
- **Persistence:** All data is real-time Firestore based.

---

## 7. Chat System
- **Current State:** 1-on-1 text messaging only.
- **Media:** Image/File sharing is **Disabled**. Do not re-enable without a dedicated Firebase Storage redesign.
- **Status:** Features "Typing..." indicators and "Online" status (1-minute window).
- **Security:** Messages are stored in Firestore with member-based security rules.

---

## 8. Profile & Image System
- **Storage:** Profile pictures are stored **locally** on the device filesystem (`filesDir/profile_pictures/`).
- **Sync:** The local path is stored in `SharedPreferences`.
- **Fallback:** If no image exists, UI generates a colored circle with user initials.
- **Rationale:** Avoids high Firebase Storage costs and latency for simple avatars.

---

## 9. AI System (FinCalc AI)
- **Engine:** Google Gemini API.
- **Modes:** Professional (Default) and **Roast Mode** (Sarcastic financial advice).
- **Initialization Rules:**
    - **Never** initialize AI services on App Startup.
    - AI must be initialized **Lazily** only when the AI Chat screen is opened.
    - API keys must be kept in `local.properties` (accessed via `BuildConfig`).

---

## 10. Settings & Preferences
- **UserPrefs:** General settings (Dark Mode, Onboarding status, Recent Tools).
- **AssistantPrefs_$uid:** AI-specific settings (Roast mode, personality, enabled state).
- **Security:** App Lock (PIN) and Biometric toggle.
- **Guest Mode:** A stateless mode that clears all data on logout.
- **Account Deletion:** Mandatory for Play Store. Deletes user profile, backups, friend settings, and blocked users from Firestore before deleting the Firebase Auth account.

---

## 11. Release History
- **v1.8 Beta:** Initial feature set.
- **v1.9.0 - v1.9.1:** Stabilization and UI polish.
- **v1.9.4:** Performance optimizations.
- **v1.9.5:** Hardened startup path (Firebase/Security fix).
- **v1.9.6:** Android 15 Edge-to-Edge compliance and Smart Scan memory fix.

---

## 12. Known Pitfalls & Regressions
- **Firebase Init:** Accessing `FirebaseAuth` before `FirebaseApp` init crashes the app. Always use safe getters.
- **Encryption Crash:** `EncryptedSharedPreferences` can fail on some devices/updates. **Must** have a regular `SharedPreferences` fallback.
- **Bitmap OOM:** Smart Scan must downsample images (target 2048px) before processing.
- **VersionCode:** Play Store requires incremental `versionCode`. Current is **17**.
- **Room Main Thread:** Never access Database on the Main Thread.

---

## 13. Coding Rules
- **Null Safety:** Avoid `!!` (force unwrap). Use `?.let`, `?:`, or `requireNotNull`.
- **Threading:** Heavy lifting (IO, DB, Image Proc) must use `Dispatchers.IO`.
- **UI State:** Prefer `collectAsStateWithLifecycle` in Compose.
- **Safety:** Wrap cloud-dependent calls (Firestore/Auth) in `try-catch` to prevent offline crashes.

---

## 14. Play Store Release Checklist
1. [ ] Increment `versionCode` in `app/build.gradle.kts`.
2. [ ] Update `versionName`.
3. [ ] Run `./gradlew clean assembleRelease bundleRelease`.
4. [ ] Verify `google-services.json` is correct.
5. [ ] Test the **Release APK** on a physical device or emulator (Clean install).
6. [ ] Check Smart Scan for memory issues with a large photo.
7. [ ] Confirm Firebase Auth works in release mode (Check SHA-1 in console).

---

## 15. Git Workflow
- **Branching:** `main` is production. Feature branches should be merged via PR.
- **Commits:** Use conventional commits:
    - `fix(scope): ...`
    - `feat(scope): ...`
    - `fix(release): ...`
- **Tags:** Tag production releases as `v1.9.x`.

---

## 16. Future AI Agent Instructions
Before modifying code:
1. Read this `BRAIN.md` in full.
2. Check `app/build.gradle.kts` for the current version.
3. Understand that **FinCalc is in Production**. Destructive changes to data models will lose user data.
4. If fixing a crash, look at `MainActivity.kt` and `SecurityUtils.kt` first to understand the startup hardening.
5. Do not "improve" code by adding complex libraries if a simple Kotlin solution exists.

---

## 17. Project Philosophy
> "FinCalc prioritizes reliability, usability, and real-world usefulness over unnecessary complexity. Every change should improve stability or user experience without sacrificing existing functionality."

---
**Last Updated:** May 2024
