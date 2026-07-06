# Build & Release Process

This document outlines the procedure for preparing a Production release of FinCalc for the Google Play Store.

## 1. Version Management
- **Location**: `app/build.gradle.kts`.
- **`versionCode`**: Must be incremented by 1 for every upload to Google Play.
- **`versionName`**: Follow semantic versioning (e.g., `1.9.6`).

## 2. Release Configuration
The `release` build type is configured in Gradle:
- `isMinifyEnabled = false`: ProGuard/R8 shrinking is currently disabled to prevent obfuscation-related crashes in critical startup paths.
- `debugSymbolLevel = "FULL"`: Ensures detailed stack traces in Play Console.

## 3. Pre-Build Checklist
1. [ ] Update `versionCode` and `versionName`.
2. [ ] Verify `local.properties` contains a valid `GEMINI_API_KEY`.
3. [ ] Run `./gradlew clean`.
4. [ ] Perform a full local test of the **Startup Path** (MainActivity -> Login/Home).
5. [ ] Verify the **Smart Scan** memory handling with a large receipt image.

## 4. Building the Artifacts
- **APK**: `./gradlew assembleRelease`
- **Bundle (AAB)**: `./gradlew bundleRelease`
- **Output Path**: `app/build/outputs/bundle/release/app-release.aab`.

## 5. Play Store Submission
- **Track**: Upload the `.aab` to the Internal, Beta, or Production track.
- **Vitals**: Monitor Android Vitals for ANRs or crashes in the first 24 hours.
- **Data Safety**: Ensure the Play Console Data Safety form matches the collections documented in `DATAMODEL.md`.

## Known Release Risks
- **Firebase Signing**: If the app crashes instantly with a "Firebase" error, verify that the SHA-1 and SHA-256 of the **Google Play App Signing Key** are added to the Firebase Console settings.
- **MasterKey Issue**: On some Android 12 updates, the KeyStore may become inaccessible. Always ensure the `SecurityUtils` fallback logic is working.
