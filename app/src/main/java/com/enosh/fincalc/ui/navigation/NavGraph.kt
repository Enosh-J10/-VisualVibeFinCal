package com.enosh.fincalc.ui.navigation

import com.enosh.fincalc.ui.screens.ChatListScreen
import com.enosh.fincalc.ui.screens.ChatRoomScreen
import com.enosh.fincalc.ui.screens.AiChatScreen
import com.enosh.fincalc.ui.screens.SmartBusinessScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enosh.fincalc.viewmodel.CurrencyViewModelFactory
import com.enosh.fincalc.ui.screens.HomeScreen
import com.enosh.fincalc.ui.screens.CurrencyConverterScreen
import com.enosh.fincalc.ui.screens.LoanCalculatorScreen
import com.enosh.fincalc.ui.screens.TipCalculatorScreen
import com.enosh.fincalc.ui.screens.TaxDiscountScreen
import com.enosh.fincalc.ui.screens.PercentageScreen
import com.enosh.fincalc.ui.screens.UnitConverterScreen
import com.enosh.fincalc.ui.screens.DateTimeScreen
import com.enosh.fincalc.ui.screens.BMIScreen
import com.enosh.fincalc.ui.screens.CalculatorScreen
import com.enosh.fincalc.ui.screens.SalaryScreen
import com.enosh.fincalc.ui.screens.NoteBookScreen
import com.enosh.fincalc.ui.screens.smartscan.SmartScanScreen
import com.enosh.fincalc.ui.screens.InsightsDashboardScreen
import com.enosh.fincalc.ui.screens.BudgetPlannerScreen
import com.enosh.fincalc.ui.screens.GoalsScreen
import com.enosh.fincalc.ui.screens.OnboardingScreen
import com.enosh.fincalc.ui.screens.SmartTravelScreen
import com.enosh.fincalc.ui.screens.TripDetailScreen
import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import com.enosh.fincalc.viewmodel.AssistantViewModel

import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    navController: NavHostController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    assistantViewModel: AssistantViewModel
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }
    val isOnboardingComplete = remember { mutableStateOf(sharedPref.getBoolean("onboarding_complete", false)) }

    NavHost(
        navController = navController,
        startDestination = if (isOnboardingComplete.value) Screen.Home.route else Screen.Onboarding.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                isDarkMode = isDarkMode,
                onNavigateToChat = { navController.navigate(Screen.ChatList.route) },
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
                        "ai_chat" -> navController.navigate(Screen.AiChat.route)
                        "insights" -> navController.navigate(Screen.Insights.route)
                        "budget" -> navController.navigate(Screen.Budget.route)
                        "goals" -> navController.navigate(Screen.Goals.route)
                        "saving_planner" -> navController.navigate(Screen.SavingPlanner.route)
                        "smart_travel" -> navController.navigate(Screen.SmartTravel.route)
                        "smart_business" -> navController.navigate(Screen.SmartBusiness.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                    }
                },
                assistantViewModel = assistantViewModel,
                financialViewModel = viewModel()
            )
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(navController, isDarkMode)
        }

        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("friendUid") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val friendUid = backStackEntry.arguments?.getString("friendUid") ?: ""
            ChatRoomScreen(chatId, friendUid, navController, isDarkMode)
        }

        composable(Screen.AiChat.route) {
            AiChatScreen(navController, isDarkMode)
        }

        composable(Screen.AiSettings.route) {
            com.enosh.fincalc.ui.screens.AiSettingsScreen(navController, isDarkMode)
        }

        composable(Screen.SmartBusiness.route) {
            SmartBusinessScreen(navController, isDarkMode)
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
        
        composable(Screen.Insights.route) { InsightsDashboardScreen(navController, isDarkMode, assistantViewModel) }
        composable(Screen.Budget.route) { BudgetPlannerScreen(navController, isDarkMode, assistantViewModel) }
        composable(Screen.Goals.route) { GoalsScreen(navController, isDarkMode, assistantViewModel) }
        composable(Screen.SavingPlanner.route) { com.enosh.fincalc.ui.screens.AutoSavingPlannerScreen(navController, isDarkMode, assistantViewModel) }
        
        composable(Screen.SmartTravel.route) { SmartTravelScreen(navController, isDarkMode, assistantViewModel) }
        composable(
            route = Screen.Friends.route,
            arguments = listOf(navArgument("search") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val initialSearch = backStackEntry.arguments?.getString("search")
            com.enosh.fincalc.ui.screens.FriendsScreen(navController, isDarkMode, initialSearch = initialSearch)
        }
        composable(
            route = Screen.TripDetail.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripDetailScreen(tripId, navController, isDarkMode)
        }

        composable(Screen.Onboarding.route) { 
            OnboardingScreen(onFinished = {
                sharedPref.edit { putBoolean("onboarding_complete", true) }
                isOnboardingComplete.value = true
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }) 
        }

        composable(
            route = Screen.Settings.route + "?resetPin={resetPin}",
            arguments = listOf(
                navArgument("resetPin") { 
                    type = NavType.BoolType
                    defaultValue = false 
                }
            )
        ) { backStackEntry ->
            val resetPin = backStackEntry.arguments?.getBoolean("resetPin") ?: false
            com.enosh.fincalc.ui.screens.SettingsScreen(
                navController = navController,
                isDarkMode = isDarkMode,
                onDarkModeChange = onDarkModeChange,
                onLogout = onLogout,
                assistantViewModel = assistantViewModel,
                initialResetPin = resetPin
            )
        }
    }
}
