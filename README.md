# FinCalc 📊

FinCalc is an Android app I built to put all the useful finance tools I need in one place. It has things like currency conversion, loan calculators, and a way to scan receipts so you don't have to type everything in.

## Features

### Tracking Money
* **Insights**: A quick look at how much you're spending and what you're spending it on.
* **Budgeting**: Set a monthly limit and the app will let you know if you're going over.
* **Goals**: A simple way to track how much you've saved for specific things.
* **Expense Log**: Keep a list of your daily spending.

### Tools & Calculators
* **Smart Scan**: Use your camera to scan receipts. It tries to find the total and category for you.
* **Currency**: Live exchange rates for a bunch of different currencies.
* **Calculators**: Basic stuff like loans, salary, taxes, and unit conversion.
* **Extras**: A BMI calculator and a simple notebook for quick notes.

### Assistant
* **Bot Character**: A little floating robot that gives tips and budget notifications. You can change how it looks in settings.

### Security
* **App Lock**: You can lock the app with a PIN or use your fingerprint.
* **Privacy**: Everything stays on your phone. Nothing is sent to a server.

## Tech Used
* **Kotlin** & **Jetpack Compose** for the UI.
* **Room** to save your data locally.
* **Retrofit** for getting currency rates.
* **ML Kit** for the receipt scanning.
* **WorkManager** for daily reminders.

## Setup
1. Clone the repo: `git clone https://github.com/Enosh-J10/FinCalc.git`
2. Open it in Android Studio.
3. Build and run it on your phone or an emulator (API 24+).

## Screenshots
Check the [screenshots](./screenshots) folder to see how it looks.

## Future Plans
* Add better charts and graphs.
* Maybe some basic AI for better spending advice.
* Support for more languages.

---
*Note: This is a student project. Don't use it for official financial advice!*
