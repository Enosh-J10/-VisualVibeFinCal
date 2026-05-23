# FinCalc – All-in-One Financial Utility App 📊

FinCalc is a modern Android financial utility application developed using **Kotlin** and **Jetpack Compose**. It provides users with a comprehensive suite of financial tools and utilities within a single platform, maintaining a clean, modern, and user-friendly fintech-style interface.

## 🚀 Features

- **Real-time Currency Converter**: Live exchange rates via API integration.
- **Financial Calculators**: Loan, Salary, Tax & Discount, Tip & Split, and Percentage calculators.
- **Smart Scan (OCR)**: Integrated Smart Scan feature for digitizing financial documents and receipts using Google ML Kit.
- **Interactive Assistant**: A floating assistant robot to help you navigate and provide smart financial insights.
- **Expense Tracking**: Built-in system to track your financial activities.
- **Health & Lifestyle**: Includes a BMI Calculator and a dedicated Notebook for financial notes.
- **Security & Customization**: App lock (PIN/Biometric), notification settings, and personalization options for the assistant and UI.
- **Modern UI**: Built with Material Design 3 and smooth Compose animations.

## 🛠 Technologies Used

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose / Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: Room (for notes, history, and expenses)
- **Networking**: Retrofit & OkHttp (for live currency rates)
- **Image Processing**: Google ML Kit (for OCR)
- **Dependency Injection**: Manual / ViewModelProvider
- **Navigation**: Compose Navigation

## 📁 Project Structure

The project follows a clean MVVM architecture:

- `data`: Handles data sources (API services, Room database, repositories).
- `domain`: Contains business logic, models, and repository interfaces.
- `ui`: Contains the UI layer (Screens, Components, Navigation).
- `viewmodel`: Contains ViewModels that bridge the domain and UI layers.
- `utils`: Helper classes for validation, security, and notifications.

## 🎯 Purpose of the Application

The main purpose of FinCalc is to simplify financial calculations and expense management by combining multiple tools into a single mobile application. It aims to provide a modern and engaging user experience through interactive features and smart automation.

## 👷 Developer

**Enosh Jaques**
*Android Application Development Project – 2026*

## 📝 Future Improvements

- [ ] AI-powered financial insights and budgeting advice.
- [ ] Voice assistant support for hands-free calculations.
- [ ] Cloud synchronization for cross-device data backup.
- [ ] Advanced analytics dashboard with charting.
- [ ] Multi-language support.

---

*Disclaimer: This app is for educational and informational purposes. Always consult with a financial professional for critical financial decisions.*
