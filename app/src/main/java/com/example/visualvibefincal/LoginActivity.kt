package com.example.visualvibefincal

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.lifecycleScope
import com.example.visualvibefincal.utils.ValidationUtils
import com.example.visualvibefincal.utils.SecurityUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.launch

/**
 * LOGIN FLOW UPDATE:
 * Now uses Firebase Authentication to sign in users.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        android.util.Log.d("LoginActivity", "Legacy Google Sign-In Result Received")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            android.util.Log.d("LoginActivity", "Legacy Google Sign-In Success")
            val firebaseCredential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            lifecycleScope.launch {
                try {
                    auth.signInWithCredential(firebaseCredential).await()
                    android.util.Log.d("LoginActivity", "Firebase Google Auth Success (Legacy)")
                    onAuthSuccess(account.email)
                } catch (e: Exception) {
                    android.util.Log.e("LoginActivity", "Firebase Google Auth Failed (Legacy)", e)
                    Toast.makeText(this@LoginActivity, "Firebase Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    findViewById<MaterialButton>(R.id.btn_google).isEnabled = true
                }
            }
        } catch (e: ApiException) {
            android.util.Log.e("LoginActivity", "Legacy Google Sign-In Failed (Code: ${e.statusCode})", e)
            val msg = when (e.statusCode) {
                7 -> "Network Error. Please check your connection."
                10 -> "Developer Error: Ensure SHA-1 and package name match in Firebase."
                12500 -> "Sign-in failed. Please update Play Services."
                12501 -> "Sign-in cancelled."
                else -> "Google Sign-In Failed (${e.statusCode})"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            findViewById<MaterialButton>(R.id.btn_google).isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            auth = FirebaseAuth.getInstance()
            android.util.Log.d("FirebaseInit", "Firebase initialized successfully in LoginActivity")
            
            // Initialize Legacy Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase configuration error. Please check app setup.", Toast.LENGTH_LONG).show()
            android.util.Log.e("FirebaseInit", "Firebase initialization failed in LoginActivity", e)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val btnGoogle = findViewById<MaterialButton>(R.id.btn_google)
        val btnGuest = findViewById<MaterialButton>(R.id.btn_guest)
        val tvSignup = findViewById<TextView>(R.id.tv_signup)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_forgot_password)
        val btnHelp = findViewById<ImageButton>(R.id.btn_help)

        btnHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_EMAIL, arrayOf("enoshjaques@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Help & Troubleshoot - Visual Vibe FinCal")
                putExtra(Intent.EXTRA_TEXT, "Hi Enosh,\n\nI need help with...")
            }
            try {
                startActivity(Intent.createChooser(intent, "Send Email"))
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.no_email_app), Toast.LENGTH_SHORT).show()
            }
        }

        fun validateInputs() {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            btnLogin.isEnabled = ValidationUtils.isValidEmail(email) && password.isNotEmpty()
        }

        etEmail.addTextChangedListener {
            tilEmail.error = null
            validateInputs()
        }
        etPassword.addTextChangedListener {
            tilPassword.error = null
            validateInputs()
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            var hasError = false
            if (!ValidationUtils.isValidEmail(email)) {
                tilEmail.error = getString(R.string.invalid_email_format)
                hasError = true
            }
            if (password.isEmpty()) {
                tilPassword.error = getString(R.string.password_empty)
                hasError = true
            }

            if (hasError) return@setOnClickListener

            // FIREBASE LOGIN
            btnLogin.isEnabled = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    btnLogin.isEnabled = true
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && !user.isEmailVerified) {
                            btnLogin.isEnabled = true
                            // Use a long-duration snackbar or dialog for verification warning
                            Snackbar.make(findViewById(android.R.id.content), 
                                getString(R.string.verify_email_warning), 
                                Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.resend_verification)) {
                                    user.sendEmailVerification().addOnCompleteListener { resendTask ->
                                        if (resendTask.isSuccessful) {
                                            Toast.makeText(this, getString(R.string.verification_email_sent), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                .show()
                            
                            showResendVerificationDialog()
                            return@addOnCompleteListener
                        }

                        onAuthSuccess(email)
                    } else {
                        val exception = task.exception
                        val message = when (exception) {
                            is FirebaseAuthInvalidUserException -> "No account found with this email."
                            is FirebaseAuthInvalidCredentialsException -> getString(R.string.invalid_credentials)
                            else -> exception?.localizedMessage ?: "Login failed."
                        }
                        
                        // Check if it's an old local account
                        val securePref = SecurityUtils.getEncryptedPrefs(this)
                        val savedEmail = securePref.getString("email", null)
                        val savedPassword = securePref.getString("password", null)
                        
                        if (email == savedEmail && password == savedPassword && !savedPassword.isNullOrEmpty()) {
                            Toast.makeText(this, "Please sign up again using our new secure system.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                        android.util.Log.e("LoginActivity", "Firebase login error", exception)
                    }
                }
        }

        btnGoogle.setOnClickListener {
            android.util.Log.d("LoginActivity", "Google sign-in button clicked")
            signInWithGoogle()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnGuest.setOnClickListener {
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit {
                putBoolean("is_guest", true)
            }
            if (SecurityUtils.isAppLockEnabled(this)) {
                val intent = Intent(this, LockActivity::class.java)
                intent.putExtra("DESTINATION", "HOME")
                startActivity(intent)
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
            }
            finish()
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun onAuthSuccess(email: String?) {
        android.util.Log.d("LoginActivity", "Authentication successful")
        SecurityUtils.skipNextLock = true
        SecurityUtils.hasAuthenticatedThisSession = true
        
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit {
            putBoolean("is_guest", false)
            putString("email", email)
            val user = FirebaseAuth.getInstance().currentUser
            putString("name", user?.displayName)
            putString("profile_pic", user?.photoUrl?.toString())
        }
        
        Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
        
        if (SecurityUtils.isAppLockEnabled(this)) {
            val intent = Intent(this, LockActivity::class.java)
            intent.putExtra("DESTINATION", "HOME")
            startActivity(intent)
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }

    private fun startLegacyGoogleSignIn() {
        android.util.Log.d("LoginActivity", "Starting legacy Google Sign-In fallback")
        getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", true) }
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun signInWithGoogle() {
        android.util.Log.d("LoginActivity", "signInWithGoogle() starting")
        val btnGoogle = findViewById<MaterialButton>(R.id.btn_google)
        btnGoogle.isEnabled = false
        Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show()

        val preferLegacy = getSharedPreferences("GooglePrefs", MODE_PRIVATE).getBoolean("PREFER_LEGACY", false)
        if (preferLegacy) {
            android.util.Log.d("LoginActivity", "Prioritizing legacy Google Sign-In due to previous timeout/failure")
            startLegacyGoogleSignIn()
            return
        }

        val credentialManager = try {
            CredentialManager.create(this)
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Failed to create CredentialManager", e)
            Toast.makeText(this, "Credential Manager Error: ${e.message}", Toast.LENGTH_LONG).show()
            btnGoogle.isEnabled = true
            return
        }
        
        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "default_web_client_id NOT FOUND", e)
            ""
        }

        if (webClientId.isEmpty()) {
            Toast.makeText(this, "Configuration Error: Web Client ID missing.", Toast.LENGTH_LONG).show()
            btnGoogle.isEnabled = true
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                android.util.Log.d("LoginActivity", "Calling getCredential with 3s timeout...")
                
                val result = withTimeoutOrNull(3000) {
                    credentialManager.getCredential(this@LoginActivity, request)
                }

                if (result == null) {
                    android.util.Log.e("LoginActivity", "Credential Manager timed out (3s)")
                    startLegacyGoogleSignIn()
                    return@launch
                }

                val credential = result.credential
                android.util.Log.d("LoginActivity", "Credential received: ${credential.type}")

                if (credential is GoogleIdTokenCredential) {
                    // Reset preference if CredentialManager succeeds
                    getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", false) }
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    android.util.Log.d("LoginActivity", "Signing into Firebase...")
                    
                    auth.signInWithCredential(firebaseCredential).await()
                    
                    val user = auth.currentUser
                    android.util.Log.d("LoginActivity", "Firebase Google Auth Success")
                    onAuthSuccess(user?.email)
                } else {
                    android.util.Log.e("LoginActivity", "Unexpected credential type: ${credential.type}")
                    Toast.makeText(this@LoginActivity, "Unexpected credential type", Toast.LENGTH_LONG).show()
                }
            } catch (_: GetCredentialCancellationException) {
                android.util.Log.w("LoginActivity", "User cancelled Google Sign-In")
                Toast.makeText(this@LoginActivity, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            } catch (e: NoCredentialException) {
                android.util.Log.e("LoginActivity", "No Google accounts found on device")
                Toast.makeText(this@LoginActivity, "No Google accounts found. Please add one in device settings.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialProviderConfigurationException) {
                android.util.Log.e("LoginActivity", "Provider configuration error: ${e.message}")
                startLegacyGoogleSignIn()
            } catch (e: GetCredentialUnsupportedException) {
                android.util.Log.e("LoginActivity", "Credential Manager unsupported: ${e.message}")
                startLegacyGoogleSignIn()
            } catch (e: GetCredentialException) {
                android.util.Log.e("LoginActivity", "Credential Manager error (${e.type}): ${e.message}")
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Unexpected error during Google Sign-In", e)
                Toast.makeText(this@LoginActivity, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnGoogle.isEnabled = true
                android.util.Log.d("LoginActivity", "Google Sign-In flow finished (btn re-enabled)")
            }
        }
    }

    private fun showResendVerificationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Email Not Verified")
            .setMessage(getString(R.string.verify_email_warning))
            .setPositiveButton(getString(R.string.resend_verification)) { _, _ ->
                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, getString(R.string.verification_email_sent), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("OK", null)
            .show()
    }
}
