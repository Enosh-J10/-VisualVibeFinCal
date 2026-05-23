@file:Suppress("Annotator")

package com.example.visualvibefincal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.ui.components.AssistantRobot
import com.example.visualvibefincal.ui.navigation.NavGraph
import com.example.visualvibefincal.ui.navigation.Screen
import com.example.visualvibefincal.utils.SecurityUtils
import com.example.visualvibefincal.utils.NotificationHelper
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import com.example.visualvibefincal.viewmodel.AssistantState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        setContent {
            val assistantViewModel: AssistantViewModel = viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                assistantViewModel.loadPrefs(context)
                delay(1000)
                assistantViewModel.showMessage("Hey! Ready to calculate? 😊", AssistantState.HAPPY)
            }
            
            var isDarkMode by remember { 
                mutableStateOf(sharedPref.getBoolean("is_dark_mode", true)) 
            }
            FinCalcTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                Box {
                    NavGraph(
                        navController = navController,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { 
                            isDarkMode = it 
                            sharedPref.edit { putBoolean("is_dark_mode", it) }
                        },
                        onLogout = {
                            startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                            finish()
                        },
                        assistantViewModel = assistantViewModel
                    )
                    AssistantRobot(
                        viewModel = assistantViewModel, 
                        isDarkMode = isDarkMode,
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FinCalcTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF00D1B2),
            background = Color(0xFF0F2027),
            surface = Color(0xFF203A43),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00D1B2),
            background = Color.White,
            surface = Color(0xFFF0F4F8),
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
