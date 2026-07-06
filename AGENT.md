# AI Agent & Developer Rules (AGENT.md)

This document contains mandatory operating rules for any AI agent or developer contributing to FinCalc. Adherence to these rules ensures production stability and consistency.

---

## 1. Safety & Stability (The "Gold" Rules)
- **Do NOT add features** unless explicitly requested in the task prompt.
- **Do NOT redesign UI** components or screens that are already functional.
- **Do NOT refactor stable systems** (e.g., Auth, Room, Smart Travel) for "style" or "modernization" without a specific bug fix or requirement.
- **Always preserve user data**: Any change to Room entities or Firestore models must be backwards compatible.
- **Backwards Safety**: Use `try-catch` or `Throwable` blocks around all startup initializers (Firebase, EncryptedSharedPreferences, Database).

---

## 2. Technical Constraints
- **Chat Attachments**: Do not reintroduce image or file sharing in the chat system. It is intentionally text-only for stability and cost management.
- **Profile Pictures**: Do not use Firebase Storage for user profile pictures. They are stored locally.
- **AI Initialization**: Never initialize the AI system or Gemini API on app startup. Use lazy initialization triggered only when the AI screen is opened.
- **Error Handling**: Never show raw Firebase exceptions, HTTP codes, or stack traces to the user. Always provide a user-friendly fallback message.
- **Null Safety**: Avoid the `!!` operator. Use Kotlin null-safety patterns (`?.let`, `?:`, etc.).

---

## 3. Safe Change Process
1. **Read the Docs**: Before making major changes, read `BRAIN.md`, `CODEBASE.md`, `DATAMODEL.md`, and `SECURITY.md`.
2. **Context Check**: Verify the current app version and `versionCode` in `app/build.gradle.kts`.
3. **Regression Prevention**: If modifying a shared utility (e.g., `UserUtils` or `SecurityUtils`), verify its usage across the entire project using `grep` or `find_usages`.
4. **Testing**: Always test changes in a **Release Build** (`assembleRelease`) on a clean install to ensure obfuscation and startup hardening work correctly.

---

## 4. Commit & Workflow
- **Commit Style**: Use conventional commits (e.g., `fix(auth): ...`, `feat(ai): ...`, `chore(release): ...`).
- **Secret Management**: Never commit `local.properties` or `google-services.json`. Ensure they remain in `.gitignore`.
- **Destructive Actions**: Any action that deletes data (Account, Trip, Note, History) MUST have an `AlertDialog` confirmation.

---

## 5. Dangerous Areas
- **`MainActivity.kt`**: This is the startup gate. Small errors here cause instant crashes for all users.
- **`SecurityUtils.kt`**: Encryption failures can lock users out of the app. Always maintain the regular `SharedPreferences` fallback.
- **`firestore.rules`**: Incorrect rules can expose all user backups or allow unauthorized trip deletions.

---
**Failure to follow these rules may result in immediate production regressions.**
