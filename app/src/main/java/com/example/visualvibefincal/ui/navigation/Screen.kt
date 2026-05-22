package com.example.visualvibefincal.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CurrencyConverter : Screen("currency")
    object LoanCalculator : Screen("loan")
    object TipCalculator : Screen("tip")
    object TaxDiscount : Screen("tax")
    object Percentage : Screen("percentage")
    object UnitConverter : Screen("unit")
    object DateTime : Screen("date_time")
    object BMI : Screen("bmi")
    object Calculator : Screen("calculator")
    object Salary : Screen("salary")
    object NoteBook : Screen("notes")
    object SmartScan : Screen("smart_scan")
    object Settings : Screen("settings")
}