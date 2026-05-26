package com.enosh.fincalc

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.content.edit
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
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.utils.SecurityUtils
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
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.launch

/**
 * SIGNUP FLOW UPDATE:
 * Now uses Firebase Authentication to create users.
 * 
 * DEVELOPER SETUP:
 * 1. Create a Firebase project at console.firebase.google.com
 * 2. Add this Android app (com.enosh.fincalc) to the project.
 * 3. Download google-services.json and place it in the app/ folder.
 * 4. Enable Email/Password provider in Firebase Console -> Authentication -> Sign-in method.
 */
class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        android.util.Log.d("SignupActivity", "Legacy Google Sign-In Result Received")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            android.util.Log.d("SignupActivity", "Legacy Google Sign-In Success")
            val firebaseCredential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            lifecycleScope.launch {
                try {
                    auth.signInWithCredential(firebaseCredential).await()
                    android.util.Log.d("SignupActivity", "Firebase Google Auth Success (Legacy)")
                    onAuthSuccess()
                } catch (e: Exception) {
                    android.util.Log.e("SignupActivity", "Firebase Google Auth Failed (Legacy)", e)
                    Toast.makeText(this@SignupActivity, "Firebase Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    findViewById<MaterialButton>(R.id.btn_google).isEnabled = true
                }
            }
        } catch (e: ApiException) {
            android.util.Log.e("SignupActivity", "Legacy Google Sign-In Failed (Code: ${e.statusCode})", e)
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
            android.util.Log.d("FirebaseInit", "Firebase initialized successfully in SignupActivity")
            
            // Initialize Legacy Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            // This happens if google-services.json is missing or Firebase is not initialized
            Toast.makeText(this, "Firebase configuration error. Please check app setup.", Toast.LENGTH_LONG).show()
            android.util.Log.e("FirebaseInit", "Firebase initialization failed in SignupActivity", e)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        val tilName = findViewById<TextInputLayout>(R.id.til_name)
        val etName = findViewById<TextInputEditText>(R.id.et_name)
        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.til_confirm_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)
        val btnSignup = findViewById<MaterialButton>(R.id.btn_signup)
        val btnGoogle = findViewById<MaterialButton>(R.id.btn_google)
        val tvLogin = findViewById<TextView>(R.id.tv_login)
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

        setupTermsClickable()

        etName.addTextChangedListener { tilName.error = null }
        etEmail.addTextChangedListener { tilEmail.error = null }
        etPassword.addTextChangedListener { tilPassword.error = null }
        etConfirmPassword.addTextChangedListener { tilConfirmPassword.error = null }

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            var hasError = false
            var firstErrorView: View? = null

            if (name.isEmpty()) {
                tilName.error = getString(R.string.name_empty)
                hasError = true
                if (firstErrorView == null) firstErrorView = etName
            }
            
            if (email.isEmpty()) {
                tilEmail.error = getString(R.string.field_cannot_be_empty)
                hasError = true
                if (firstErrorView == null) firstErrorView = etEmail
            } else if (!ValidationUtils.isValidEmail(email)) {
                tilEmail.error = getString(R.string.invalid_email_format)
                hasError = true
                if (firstErrorView == null) firstErrorView = etEmail
            }
            
            if (password.isEmpty()) {
                tilPassword.error = getString(R.string.password_empty)
                hasError = true
                if (firstErrorView == null) firstErrorView = etPassword
            } else if (!ValidationUtils.isValidPassword(password)) {
                tilPassword.error = getString(R.string.password_too_short)
                hasError = true
                if (firstErrorView == null) firstErrorView = etPassword
            }
            
            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = getString(R.string.field_cannot_be_empty)
                hasError = true
                if (firstErrorView == null) firstErrorView = etConfirmPassword
            } else if (password != confirmPassword) {
                tilConfirmPassword.error = getString(R.string.passwords_dont_match)
                hasError = true
                if (firstErrorView == null) firstErrorView = etConfirmPassword
            }

            if (hasError) {
                firstErrorView?.requestFocus()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, getString(R.string.signup_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // FIREBASE SIGNUP
            btnSignup.isEnabled = false
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    btnSignup.isEnabled = true
                    if (task.isSuccessful) {
                        // Success: Save non-sensitive data locally
                        val securePref = SecurityUtils.getEncryptedPrefs(this)
                        securePref.edit {
                            putString("name", name)
                            putString("email", email)
                            putBoolean("has_account", true)
                            // Remove any old local password if it exists
                            remove("password")
                        }
                        
                        android.util.Log.d("SignupActivity", "Firebase user created successfully")
                        
                        // Send verification email
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    // Use Dialog or long Snackbar for verification message
                                    androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("Verification Sent")
                                        .setMessage(getString(R.string.verification_email_sent))
                                        .setPositiveButton("OK") { _, _ ->
                                            startActivity(Intent(this, LoginActivity::class.java))
                                            finish()
                                        }
                                        .setCancelable(false)
                                        .show()
                                } else {
                                    Toast.makeText(this, "Failed to send verification email: ${verifyTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    // Still go to login, they can resend from there
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                            }
                    } else {
                        // Failure: Show user-friendly error
                        val exception = task.exception
                        val message = when (exception) {
                            is FirebaseAuthWeakPasswordException -> getString(R.string.password_too_short)
                            is FirebaseAuthInvalidCredentialsException -> getString(R.string.invalid_email_format)
                            is FirebaseAuthUserCollisionException -> "This email is already registered."
                            else -> exception?.localizedMessage ?: "Signup failed. Please try again."
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        android.util.Log.e("SignupActivity", "Firebase signup error", exception)
                    }
                }
        }

        btnGoogle.setOnClickListener {
            android.util.Log.d("SignupActivity", "Google sign-in button clicked")
            signInWithGoogle()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun startLegacyGoogleSignIn() {
        android.util.Log.d("SignupActivity", "Starting legacy Google Sign-In fallback")
        getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", true) }
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun signInWithGoogle() {
        android.util.Log.d("SignupActivity", "signInWithGoogle() starting")
        val btnGoogle = findViewById<MaterialButton>(R.id.btn_google)
        btnGoogle.isEnabled = false
        Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show()

        val preferLegacy = getSharedPreferences("GooglePrefs", MODE_PRIVATE).getBoolean("PREFER_LEGACY", false)
        if (preferLegacy) {
            android.util.Log.d("SignupActivity", "Prioritizing legacy Google Sign-In due to previous timeout/failure")
            startLegacyGoogleSignIn()
            return
        }

        val credentialManager = try {
            CredentialManager.create(this)
        } catch (e: Exception) {
            android.util.Log.e("SignupActivity", "Failed to create CredentialManager", e)
            Toast.makeText(this, "Credential Manager Error: ${e.message}", Toast.LENGTH_LONG).show()
            btnGoogle.isEnabled = true
            return
        }
        
        val webClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            android.util.Log.e("SignupActivity", "default_web_client_id NOT FOUND", e)
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
                android.util.Log.d("SignupActivity", "Calling getCredential with 3s timeout...")
                
                val result = withTimeoutOrNull(3000) {
                    credentialManager.getCredential(this@SignupActivity, request)
                }

                if (result == null) {
                    android.util.Log.e("SignupActivity", "Credential Manager timed out (3s)")
                    startLegacyGoogleSignIn()
                    return@launch
                }

                val credential = result.credential
                android.util.Log.d("SignupActivity", "Credential received: ${credential.type}")

                if (credential is GoogleIdTokenCredential) {
                    // Reset preference if CredentialManager succeeds
                    getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", false) }
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    android.util.Log.d("SignupActivity", "Signing into Firebase...")
                    
                    auth.signInWithCredential(firebaseCredential).await()
                    
                    android.util.Log.d("SignupActivity", "Firebase Google Auth Success")
                    onAuthSuccess()
                } else {
                    android.util.Log.e("SignupActivity", "Unexpected credential type: ${credential.type}")
                    Toast.makeText(this@SignupActivity, "Unexpected credential type", Toast.LENGTH_LONG).show()
                }
            } catch (_: GetCredentialCancellationException) {
                android.util.Log.w("SignupActivity", "User cancelled Google Sign-In")
                Toast.makeText(this@SignupActivity, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            } catch (e: NoCredentialException) {
                android.util.Log.e("SignupActivity", "No Google accounts found on device")
                Toast.makeText(this@SignupActivity, "No Google accounts found. Please add one in device settings.", Toast.LENGTH_LONG).show()
            } catch (e: GetCredentialProviderConfigurationException) {
                android.util.Log.e("SignupActivity", "Provider configuration error: ${e.message}")
                startLegacyGoogleSignIn()
            } catch (e: GetCredentialUnsupportedException) {
                android.util.Log.e("SignupActivity", "Credential Manager unsupported: ${e.message}")
                startLegacyGoogleSignIn()
            } catch (e: GetCredentialException) {
                android.util.Log.e("SignupActivity", "Credential Manager error (${e.type}): ${e.message}")
                Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("SignupActivity", "Unexpected error during Google Sign-In", e)
                Toast.makeText(this@SignupActivity, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnGoogle.isEnabled = true
                android.util.Log.d("SignupActivity", "Google Sign-In flow finished (btn re-enabled)")
            }
        }
    }

    private fun onAuthSuccess() {
        val user = auth.currentUser
        SecurityUtils.skipNextLock = true
        SecurityUtils.hasAuthenticatedThisSession = true
        
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit {
            putBoolean("is_guest", false)
            putString("email", user?.email)
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

    private fun setupTermsClickable() {
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)
        val fullText = getString(R.string.agree_terms)
        val spannableString = SpannableString(fullText)

        val termsAndConditions = "Terms & Conditions"
        val startIndex = fullText.indexOf(termsAndConditions)
        if (startIndex != -1) {
            val endIndex = startIndex + termsAndConditions.length
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@SignupActivity, TermsActivity::class.java))
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = "#00D1B2".toColorInt()
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        cbTerms.text = spannableString
        cbTerms.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }
}
