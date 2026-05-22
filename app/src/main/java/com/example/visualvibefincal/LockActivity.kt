package com.example.visualvibefincal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
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

class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val destination = intent.getStringExtra("DESTINATION") ?: "HOME"
        val isReturnToApp = intent.getBooleanExtra("is_return_to_app", false)
        
        setContent {
            LockScreen(
                onSuccess = {
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
            )
        }

        if (SecurityUtils.isBiometricEnabled(this)) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val isReturnToApp = intent.getBooleanExtra("is_return_to_app", false)
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (isReturnToApp) {
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val destination = intent.getStringExtra("DESTINATION") ?: "HOME"
                        val intent = when(destination) {
                            "HOME" -> Intent(this@LockActivity, HomeActivity::class.java)
                            "LOGIN" -> Intent(this@LockActivity, LoginActivity::class.java)
                            else -> Intent(this@LockActivity, HomeActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinCal")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun LockScreen(onSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val savedPin = SecurityUtils.getAppPin(context) ?: "1234"

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Visual Vibe FinCal", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter your 4-digit PIN", fontSize = 16.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = pin,
            onValueChange = { 
                if (it.length <= 4) pin = it
                if (it.length == 4) {
                    if (it == savedPin) {
                        onSuccess()
                    } else {
                        error = true
                        pin = ""
                    }
                }
            },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.width(150.dp),
            isError = error
        )
        
        if (error) {
            Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}