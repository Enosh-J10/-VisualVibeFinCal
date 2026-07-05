# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.9.6] - 2024-05-25
### Added
- Full support for Android 15 edge-to-edge display standards.
- Memory-efficient bitmap downsampling for Smart Scan (Receipt OCR).

### Fixed
- Hardened startup sequence to prevent instant crashes in Play Store builds.
- Implemented global `Throwable` wrappers around Firebase and Room initializers.
- Improved `EncryptedSharedPreferences` reliability with a regular `SharedPreferences` fallback mechanism.

### Changed
- Stabilized release build configuration for production readiness.

## [1.9.5] - 2024-05-15
### Fixed
- Addressed startup crash issues reported by Play Store testers.
- Hardened Firebase initialization path.
- Enhanced Room database startup safety during user session restoration.

## [1.9.4] - 2024-05-01
### Changed
- Prepared application for first production release.
- Updated Google Play Store listings and descriptions.
- Cleaned up photo and video permissions to comply with latest Play Store policies.

## [1.9.2] - 2024-04-10
### Added
- Open Testing track preparation.

### Fixed
- Miscellaneous bugs identified during initial beta feedback.
- Updated versioning logic for incremental Play Console uploads.

## [1.9.0] - 2024-03-20
### Added
- **Friends System**: Search and connect using FinCalc IDs.
- **Text Chat**: Real-time 1-on-1 messaging with typing indicators.
- **Smart Business**: Professional metrics tracking.
- **Cloud Backup & Restore**: Secure Firestore-based data snapshots.
- **Profile Pictures**: Locally managed user avatars.
- **Fun Local Reminders**: Gamified notifications for financial tracking.

### Changed
- Improved Smart Travel collaborative logic.
- Enhanced AI Assistant responses and persona settings.
- Polished UI across all budgeting and calculator screens.
- Refined Guest Mode to handle data isolation more effectively.

## [1.8-beta] - 2024-01-15
### Added
- Closed Testing release for core feature set.
- Initial implementation of Friends and Smart Travel modules.
- First-generation AI Assistant (Gemini integration).
- Baseline local backup/restore functionality.

### Fixed
- Significant bug fixes following internal testing phase.
