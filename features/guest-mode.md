# Guest Mode & Data Isolation

FinCalc offers a "Try Before You Buy" experience through Guest Mode, allowing users to use local tools without an account.

## Mechanism
- **UID**: The app uses a hardcoded UID of `"guest"`.
- **Database**: Local data is stored in `fincalc_database_guest.db`.
- **Preferences**: Settings are saved in `UserPrefs` under guest-specific keys.

## Feature Restrictions
| Feature | Guest Access | Reason |
| :--- | :---: | :--- |
| Basic Calculators | YES | Fully local. |
| Smart Scan | YES | Local processing. |
| Chat | NO | Requires Firestore auth. |
| Smart Travel | NO | Collaborative cloud features require UID. |
| AI Assistant | LIMITED | Professional mode allowed; Roast/History limited. |
| Backup/Restore | NO | Requires private cloud document. |

## Data Persistence
- **Session**: Guest data persists across app restarts as long as the user does not log out.
- **Wipe**: Clicking "Leave Guest Mode" triggers a **Nuclear Clear**:
    - Purges the guest database.
    - Clears guest `SharedPreferences`.
    - Deletes guest-scoped cache and temp files.

## Development Rules
- Always check `isGuest` before showing cloud-related UI components.
- Do not attempt to sync guest data to Firestore under a generic document.
