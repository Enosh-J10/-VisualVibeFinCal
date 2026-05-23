package com.example.visualvibefincal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.viewmodel.CurrencyViewModelFactory
import com.example.visualvibefincal.ui.screens.HomeScreen
import com.example.visualvibefincal.ui.screens.CurrencyConverterScreen
import com.example.visualvibefincal.ui.screens.LoanCalculatorScreen
import com.example.visualvibefincal.ui.screens.TipCalculatorScreen
import com.example.visualvibefincal.ui.screens.TaxDiscountScreen
import com.example.visualvibefincal.ui.screens.PercentageScreen
import com.example.visualvibefincal.ui.screens.UnitConverterScreen
import com.example.visualvibefincal.ui.screens.DateTimeScreen
import com.example.visualvibefincal.ui.screens.BMIScreen
import com.example.visualvibefincal.ui.screens.CalculatorScreen
import com.example.visualvibefincal.ui.screens.SalaryScreen
import com.example.visualvibefincal.ui.screens.NoteBookScreen
import com.example.visualvibefincal.ui.screens.smartscan.SmartScanScreen

import com.example.visualvibefincal.viewmodel.AssistantViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    assistantViewModel: AssistantViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                isDarkMode = isDarkMode,
                onLogout = onLogout,
                onNavigateToTool = { toolId ->
                    when (toolId) {
                        "curr" -> navController.navigate(Screen.CurrencyConverter.route)
                        "loan" -> navController.navigate(Screen.LoanCalculator.route)
                        "tip" -> navController.navigate(Screen.TipCalculator.route)
                        "tax" -> navController.navigate(Screen.TaxDiscount.route)
                        "perc" -> navController.navigate(Screen.Percentage.route)
                        "unit" -> navController.navigate(Screen.UnitConverter.route)
                        "date" -> navController.navigate(Screen.DateTime.route)
                        "bmi" -> navController.navigate(Screen.BMI.route)
                        "calc" -> navController.navigate(Screen.Calculator.route)
                        "salary" -> navController.navigate(Screen.Salary.route)
                        "notes" -> navController.navigate(Screen.NoteBook.route)
                        "smart_scan" -> navController.navigate(Screen.SmartScan.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                    }
                },
                assistantViewModel = assistantViewModel
            )
        }
        composable(Screen.CurrencyConverter.route) { 
            CurrencyConverterScreen(
                navController = navController, 
                isDarkMode = isDarkMode,
                viewModel = viewModel(factory = CurrencyViewModelFactory()),
                assistantViewModel = assistantViewModel
            ) 
        }
        composable(Screen.LoanCalculator.route) { 
            LoanCalculatorScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) 
        }
        composable(Screen.TipCalculator.route) { TipCalculatorScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.TaxDiscount.route) { TaxDiscountScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.Percentage.route) { PercentageScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.UnitConverter.route) { UnitConverterScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.DateTime.route) { DateTimeScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.BMI.route) { BMIScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.Calculator.route) { CalculatorScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.Salary.route) { SalaryScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.NoteBook.route) { NoteBookScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        composable(Screen.SmartScan.route) { SmartScanScreen(navController, isDarkMode, assistantViewModel = assistantViewModel) }
        
        composable(Screen.Settings.route) {
            // Reusing the Settings content from HomeScreen's Dialog if possible, 
            // but for a dedicated screen, we might want a slightly different layout.
            // For now, let's keep it simple or create a new SettingsScreen.
            com.example.visualvibefincal.ui.screens.SettingsScreen(
                navController = navController,
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                assistantViewModel = assistantViewModel
            )
        }
    }
}
