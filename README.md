# FinCalc – Smart Finance, AI & Travel Companion

FinCalc is a comprehensive Android personal finance application designed to simplify budgeting, expense tracking, and financial planning. Designed for both casual users and finance enthusiasts, it features an intuitive interface built with Material 3, AI-powered assistance, shared travel expenses, and smart business tracking.

## Key Features

- **Budget Planner**: Set monthly limits and monitor spending trends.
- **Expense Tracker**: Log daily transactions with categorization.
- **Savings Goals**: Track progress toward long-term financial targets.
- **Auto Saving Planner**: Automated calculations for reaching goals.
- **Smart Travel**: Collaborative trip expense management with friends.
- **Smart Business**: Professional tools for tracking business income and targets.
- **AI Assistant**: Personalized financial tips and "Roast Mode" powered by Google Gemini.
- **Friends System**: Securely connect with other users via unique FinCalc IDs.
- **Text Chat**: Secure 1-on-1 messaging with real-time status indicators.
- **Smart Scan**: OCR-powered receipt scanning for automated entry.
- **Notes**: Integrated notebook for financial reminders and checklists.
- **Cloud Backup & Restore**: Securely sync your data across devices using Firestore.
- **Profile Pictures**: Local avatar management with initials-based fallback.
- **Fun Local Reminders**: Engaging notifications to keep you financially active.
- **Unit & Currency Converters**: Real-time exchange rates and measurement swaps.
- **Financial Calculators**: Dedicated tools for Loans, Salary, Tax, and Tips.
- **Guest Mode**: Full local functionality without requiring an account.

## Usage Tips

- **AI Assistant**: Try enabling **Roast Mode** in the settings for a more playful and sarcastic financial coaching experience!
- **Smart Travel**: Use the collaborative features to split dinner or hotel costs instantly with friends.
- **Smart Scan**: For the best results, ensure receipts are well-lit and laid flat before scanning.
- **Currency Converter**: Use the swap button between currency selectors to instantly reverse your conversion direction.

## Screenshots

![Home](screenshots/home.png)
![Smart Travel](screenshots/smart-travel.png)
![AI Assistant](screenshots/ai-assistant.png)

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Authentication**: Firebase Authentication
- **Cloud Database**: Cloud Firestore
- **Local Database**: Room
- **Background Tasks**: WorkManager
- **Generative AI**: Google Gemini API
- **Media**: Android Photo Picker & Coil

## Architecture Overview

FinCalc follows the **MVVM (Model-View-ViewModel)** architectural pattern to ensure a clean separation of concerns and high maintainability.

- **Jetpack Compose**: 100% declarative UI for a modern, responsive experience.
- **ViewModels**: Manage UI state and encapsulate business logic.
- **Room Database**: Robust local persistence for offline access and user-scoped data isolation.
- **Firebase Integration**: Secure authentication and real-time cloud synchronization via Firestore.
- **User-Scoped Data**: Local databases are unique to each UID to prevent data leakage.
- **Local Storage**: Profile pictures are stored on the device filesystem to minimize latency and cloud costs.

## Firebase Collections

- `users`: Core profile data (UID, email, FinCalc ID).
- `friends`: Peer-to-peer friendship records.
- `friendRequests`: Pending and historical connection status.
- `chats`: Metadata and presence status for messaging rooms.
- `trips`: Shared "Smart Travel" trip records.
- `expenses`: Transaction data for individual and shared use.
- `flags`: Dispute markers for shared trip expenses.
- `backups`: Encrypted JSON snapshots of user application data.

## Important Design Decisions

- **Stability First**: Production releases prioritize crash prevention and data integrity over experimental feature sets.
- **Text-Only Chat**: Messaging is currently optimized for text for maximum stability; image sharing is disabled.
- **Local Profile Images**: Avatars are managed locally with a deterministic initials-based fallback.
- **Spark-Plan Optimized**: Built to run efficiently without requiring paid Firebase Cloud Functions.
- **Lazy AI Initialization**: AI services are initialized only when the AI screen is accessed to optimize battery and memory.

## Installation / Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/Enosh-J10/FinCalc.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Place your `google-services.json` file in the `app/` directory.
4. Add your `GEMINI_API_KEY` to `local.properties` (e.g., `GEMINI_API_KEY=your_key_here`).
5. Sync Gradle and build the project.

## Build Commands

```bash
./gradlew clean
./gradlew assembleRelease
./gradlew bundleRelease
```

## Release Notes

Latest Version: **v1.9.6**

## Privacy & Disclaimer

- FinCalc is not a bank or regulated financial institution.
- This application does not provide professional financial, investment, or legal advice.
- All calculations are estimates provided for informational purposes.
- Users assume full responsibility for their financial decisions and data management.

## License

License: All rights reserved unless otherwise specified.

## Maintainer

**Enosh Jaques**
