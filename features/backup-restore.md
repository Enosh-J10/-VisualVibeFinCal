# Data Backup & Restoration

FinCalc ensures user data safety through two primary backup mechanisms: Cloud Sync and Local JSON Export.

## Cloud Backup
- **Storage**: Full app state is serialized into a JSON string and stored in a single Firestore document at `users/{uid}/backups/latest`.
- **Content**: Includes all Room entities (Expenses, Goals, Budgets, Business, AI History) and non-sensitive `SharedPreferences`.
- **Security**: Backups are strictly owner-only. Encryption tokens and passwords are **excluded** during serialization.

## Local Backup
- **Format**: `.json` file.
- **Provider**: Uses the Android Storage Access Framework (SAF) to let users save files to their local storage or SD card.
- **Workflow**:
    - **Export**: `BackupUtils.createBackupData(context)` -> `Gson.toJson()` -> `OutputStream`.
    - **Import**: `BackupUtils.importData(uri)` -> `Gson.fromJson()` -> `AppDatabase.insert`.

## Restoration Safety
- **Overwrite**: Importing a backup overwrites local data. A confirmation dialog is required.
- **Validation**: The JSON structure is validated during deserialization. If the file is corrupt, the local database remains untouched.
- **UID Match**: Cloud restore only works for the currently authenticated UID.

## Classes
- **`BackupRestoreScreen.kt`**: UI for triggering backup/restore.
- **`BackupViewModel.kt`**: State management for loading indicators.
- **`BackupUtils.kt`**: The "brain" of the serialization logic.

## Pitfalls to Avoid
- Never include `MasterKey` aliases or `app_pin` in the backup JSON.
- Ensure the Room database is not closed while a backup is in progress.
