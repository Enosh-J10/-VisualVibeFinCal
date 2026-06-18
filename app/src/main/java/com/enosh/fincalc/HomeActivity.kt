@file:Suppress("Annotator")

package com.enosh.fincalc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enosh.fincalc.ui.theme.FinCalcTheme
import com.enosh.fincalc.ui.components.AssistantRobot
import com.enosh.fincalc.ui.navigation.NavGraph
import com.enosh.fincalc.ui.navigation.Screen
import com.enosh.fincalc.utils.SecurityUtils
import com.enosh.fincalc.utils.NotificationHelper
import com.enosh.fincalc.utils.UserUtils
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.AssistantState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class HomeActivity : ComponentActivity() {
    private var isLocked = false

    private val lockActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            isLocked = false
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (SecurityUtils.isAppLockEnabled(this) && !isLocked) {
                    if (SecurityUtils.skipNextLock) {
                        SecurityUtils.skipNextLock = false
                        SecurityUtils.hasAuthenticatedThisSession = true
                        return@LifecycleEventObserver
                    }
                    
                    if (SecurityUtils.hasAuthenticatedThisSession) {
                        return@LifecycleEventObserver
                    }

                    val intent = Intent(this, LockActivity::class.java).apply {
                        putExtra("is_return_to_app", true)
                    }
                    lockActivityResultLauncher.launch(intent)
                    isLocked = true
                }
            }
        })
        
        // Ask for permission to show notifications on newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }

        enableEdgeToEdge()
        val sharedPref = getSharedPreferences(UserUtils.PREFS_NAME, MODE_PRIVATE)
        val openSettings = intent.getBooleanExtra("OPEN_SETTINGS", false)
        val resetPin = intent.getBooleanExtra("RESET_PIN", false)
        val deepLinkUri = intent.data

        @OptIn(ExperimentalLayoutApi::class)
        setContent {
            val assistantViewModel: AssistantViewModel = viewModel()
            val context = LocalContext.current
            val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "guest" }
            val darkModeKey = remember(uid) { UserUtils.getScopedKey(uid, "is_dark_mode") }
            
            var isDarkMode by remember(darkModeKey) { 
                mutableStateOf(sharedPref.getBoolean(darkModeKey, true)) 
            }
            FinCalcTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    assistantViewModel.loadPrefs(context)
                    
                    val isGuest = sharedPref.getBoolean("is_guest", false)
                    if (!isGuest && FirebaseAuth.getInstance().currentUser != null) {
                        try {
                            UserUtils.ensureFinCalcUserProfile(context)
                        } catch (e: Exception) {
                            android.util.Log.e("FinCalc", "Profile sync failed: ${e.message}")
                        }
                    }

                    delay(1000)
                    assistantViewModel.showMessage("Hey! Ready to calculate? 😊", AssistantState.HAPPY)
                    
                    if (openSettings) {
                        navController.navigate(Screen.Settings.route + "?resetPin=$resetPin")
                    }
                    
                    deepLinkUri?.let { uri ->
                        if (uri.scheme == "fincalc" && uri.host == "add-friend") {
                            val friendId = uri.getQueryParameter("id")
                            if (friendId != null) {
                                navController.navigate(Screen.Friends.route + "?search=$friendId")
                            }
                        }
                    }
                }

                Box {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    NavGraph(
                        navController = navController,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { 
                            isDarkMode = it 
                            sharedPref.edit { putBoolean(darkModeKey, it) }
                        },
                        onLogout = {
                            UserUtils.logout(this@HomeActivity) {
                                startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                                finish()
                            }
                        },
                        assistantViewModel = assistantViewModel
                    )
                    
                    val excludedRoutes = listOf(
                        Screen.AiChat.route,
                        Screen.AiSettings.route,
                        Screen.ChatRoom.route,
                        Screen.ChatList.route,
                        Screen.Friends.route,
                        Screen.Onboarding.route
                    )
                    
                    val isImeVisible = WindowInsets.isImeVisible
                    
                    if (currentRoute !in excludedRoutes && !isImeVisible) {
                        AssistantRobot(
                            viewModel = assistantViewModel, 
                            isDarkMode = isDarkMode,
                            onOpenSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onOpenAiChat = {
                                navController.navigate(Screen.AiChat.route)
                            }
                        )
                    }
                }
            }
        }
    }
}
