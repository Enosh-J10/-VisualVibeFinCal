# FinCalc – All-in-One Financial Utility App 📊

FinCalc is a professional-grade, modern Android financial utility application built with **Kotlin** and **Jetpack Compose**. It provides users with a comprehensive suite of financial tools, smart expense tracking, and AI-driven insights, all within a clean, Material 3 interface.

## 🚀 Key Features

### 💰 Financial Planning & Tracking
- **Insights Dashboard**: Visual summaries of your financial health, including monthly spending trends and category-wise breakdowns.
- **Budget Planner**: Set monthly spending limits and receive real-time warnings and progress updates.
- **Savings Goals**: Create and track multiple savings targets with visual progress bars.
- **Expense Tracking**: Easily log and manage your daily expenses.

### 🧮 Smart Tools & Calculators
- **Smart Scan (OCR)**: Automatically digitize receipts and bills using Google ML Kit. Detects totals, merchants, and categories automatically.
- **Real-time Currency Converter**: Live exchange rates for over 150 currencies via API integration.
- **Versatile Calculators**: Loan, Salary, Tax & Discount, Tip & Split, Percentage, and Unit converters.
- **Personal Tools**: Integrated BMI Calculator, Date/Time tools, and a secure Notebook.

### 🤖 Intelligent Assistant
- **Interactive Robot**: A persistent, customizable floating assistant that provides smart financial suggestions, guidance, and real-time feedback on your budget.
- **Smart Suggestions**: Rule-based AI tips based on your spending habits and financial data.

### 🛡️ Security & Privacy
- **App Lock**: Protect your data with a secure 4-digit PIN or Biometric authentication.
- **Privacy First**: All sensitive data is stored locally on your device.
- **Trust Indicators**: Transparent explanations of camera and biometric usage.

## 🛠 Technologies Used

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose / Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: Room (Expenses, Goals, Budgets, History)
- **Networking**: Retrofit & OkHttp (Currency Rates)
- **AI/ML**: Google ML Kit (Text Recognition)
- **Background Tasks**: WorkManager (Daily Reminders)
- **Local Storage**: SharedPreferences (Preferences & Settings)
- **Navigation**: Jetpack Compose Navigation

## 📁 Project Structure

```text
com.example.visualvibefincal
├── data
│   ├── local      # Room Database, DAOs, and Entities
│   ├── model      # API Data Models
│   └── repository # Data access implementations
├── domain         # Business logic models
├── ui
│   ├── components # Reusable Compose UI elements
│   ├── navigation # NavGraph and Screen definitions
│   └── screens    # All feature screens (SmartScan, Insights, etc.)
├── utils          # Helpers for validation, security, and backup
└── viewmodel      # MVVM ViewModels
```

## ⚙️ Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Enosh-J10/FinCalc.git
   ```
2. **Open in Android Studio**:
   - Open the root folder in Android Studio (Ladybug or newer recommended).
3. **Sync Gradle**:
   - Let the project sync to download all dependencies.
4. **Run the App**:
   - Connect an Android device or start an emulator (API 24+).
   - Click the "Run" button.

## 📸 Screenshots

*Check the [screenshots](./screenshots) folder for more details.*

| Home Screen | Insights Dashboard | Smart Scan |
| :---: | :---: | :---: |
| [Placeholder] | [Placeholder] | [Placeholder] |

## 📦 Release Information
- **Version**: v1.0 Beta
- **Status**: Stable Build
- **Notes**: Initial release featuring Smart Scan AI, Budget Planning, and the Interactive Assistant.

## 👷 Developer

**Enosh Jaques**
*Android Application Development Project – 2026*

## 📝 Future Improvements

- [ ] AI-powered advanced financial forecasting.
- [ ] Cloud synchronization with end-to-end encryption.
- [ ] Multi-currency budget support.
- [ ] Voice-activated assistant interactions.
- [ ] Dark mode dynamic theme refinement.

---

*Disclaimer: This app is for educational purposes. Always consult with a financial professional for critical decisions.*
