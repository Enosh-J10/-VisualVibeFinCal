package com.example.visualvibefincal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.visualvibefincal.utils.SecurityUtils
import com.example.visualvibefincal.ui.theme.FinCalcTheme

class LockActivity : FragmentActivity() {
    private var hasPrompted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasPrompted = savedInstanceState?.getBoolean("HAS_PROMPTED") ?: false
        
        val destination = intent.getStringExtra("DESTINATION") ?: "HOME"
        val isReturnToApp = intent.getBooleanExtra("is_return_to_app", false)
        
        setContent {
            FinCalcTheme {
                LockScreen(
                    onSuccess = {
                        handleSuccess(isReturnToApp, destination)
                    }
                )
            }
        }

        if (SecurityUtils.isBiometricEnabled(this) && !hasPrompted) {
            showBiometricPrompt(isReturnToApp, destination)
            hasPrompted = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("HAS_PROMPTED", hasPrompted)
    }

    private fun handleSuccess(isReturnToApp: Boolean, destination: String) {
        if (isReturnToApp) {
            setResult(RESULT_OK)
            finish()
        } else {
            val intent = when(destination) {
                "HOME" -> Intent(this, HomeActivity::class.java)
                "LOGIN" -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, HomeActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showBiometricPrompt(isReturnToApp: Boolean, destination: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleSuccess(isReturnToApp, destination)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If biometric is cancelled or fails, we just stay on PIN screen
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinCal")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun LockScreen(onSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Background brush to match app theme
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val backgroundBrush = if (isDarkMode) {
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F4F8)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Visual Vibe FinCal", 
                fontSize = 32.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = Color(0xFF00D1B2)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Security Locked", 
                fontSize = 20.sp, 
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            
            Text(
                "Enter your 4-digit PIN to continue", 
                fontSize = 14.sp, 
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = pin,
                onValueChange = { 
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        pin = it
                        error = false
                        if (it.length == 4) {
                            if (SecurityUtils.verifyPin(context, it)) {
                                onSuccess()
                            } else {
                                error = true
                                pin = ""
                            }
                        }
                    }
                },
                label = { Text("Enter PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(200.dp),
                isError = error,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp, 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00D1B2),
                    unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f),
                    errorBorderColor = Color.Red,
                    cursorColor = Color(0xFF00D1B2)
                )
            )
            
            if (error) {
                Text(
                    "Incorrect PIN. Try again.", 
                    color = Color.Red, 
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            if (SecurityUtils.isBiometricEnabled(context)) {
                TextButton(onClick = { 
                    // This will trigger the biometric prompt again if the activity is not finished
                    (context as? LockActivity)?.let {
                        // Re-trigger biometric prompt logic could be here if needed
                    }
                }) {
                    Text("Use Biometric", color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
