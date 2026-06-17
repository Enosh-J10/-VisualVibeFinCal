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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.lifecycleScope
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.utils.SecurityUtils
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SIGNUP FLOW UPDATE:
 * Optimized for stability and performance. Prevents main thread blocking.
 */
class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var accentTeal: Int = 0

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val firebaseCredential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            lifecycleScope.launch {
                setLoading(true)
                try {
                    auth.signInWithCredential(firebaseCredential).await()
                    onAuthSuccess()
                } catch (e: Exception) {
                    android.util.Log.e("SignupActivity", "Firebase Google Auth Failed", e)
                    Toast.makeText(this@SignupActivity, "Firebase Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false)
                }
            }
        } catch (e: ApiException) {
            android.util.Log.e("SignupActivity", "Google Sign-In Failed (Code: ${e.statusCode})", e)
            val msg = when (e.statusCode) {
                7 -> "Network Error. Please check your connection."
                10 -> "Developer Error: Ensure SHA-1 and package name match in Firebase."
                12500 -> "Sign-in failed. Please update Play Services."
                12501 -> "Sign-in cancelled."
                else -> "Google Sign-In Failed (${e.statusCode})"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            setLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        accentTeal = try { "#00D1B2".toColorInt() } catch (_: Exception) { 0xFF00D1B2.toInt() }

        try {
            auth = FirebaseAuth.getInstance()
            
            // Initialize Legacy Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase configuration error.", Toast.LENGTH_LONG).show()
            android.util.Log.e("FirebaseInit", "Firebase initialization failed", e)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        // Pre-initialize EncryptedSharedPreferences off the main thread to avoid later hitches
        lifecycleScope.launch(Dispatchers.IO) {
            SecurityUtils.getEncryptedPrefs(applicationContext)
        }

        val tilName = findViewById<TextInputLayout>(R.id.til_name)
        val etName = findViewById<TextInputEditText>(R.id.et_name)
        val tilEmail = findViewById<TextInputLayout>(R.id.til_email)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val tilPassword = findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.til_confirm_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnSignup = findViewById<MaterialButton>(R.id.btn_signup)
        val btnGoogle = findViewById<MaterialButton>(R.id.btn_google)
        val tvLogin = findViewById<TextView>(R.id.tv_login)
        val btnHelp = findViewById<ImageButton>(R.id.btn_help)

        btnHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:".toUri()
                putExtra(Intent.EXTRA_EMAIL, arrayOf("enoshjaques@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Help & Troubleshoot - Visual Vibe FinCal")
                putExtra(Intent.EXTRA_TEXT, "Hi Enosh,\n\nI need help with signup...")
            }
            try {
                startActivity(Intent.createChooser(intent, "Send Email"))
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.no_email_app), Toast.LENGTH_SHORT).show()
            }
        }

        setupTermsClickable()

        // Lightweight listeners to clear errors only
        etName.addTextChangedListener { tilName.error = null }
        etEmail.addTextChangedListener { tilEmail.error = null }
        etPassword.addTextChangedListener { tilPassword.error = null }
        etConfirmPassword.addTextChangedListener { tilConfirmPassword.error = null }

        btnSignup.setOnClickListener {
            performSignup()
        }

        btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        val btnSignup = findViewById<MaterialButton>(R.id.btn_signup)
        val btnGoogle = findViewById<MaterialButton>(R.id.btn_google)
        val progressBar = findViewById<ProgressBar>(R.id.progress_signup)

        btnSignup.isEnabled = !isLoading
        btnGoogle.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun performSignup() {
        val etName = findViewById<TextInputEditText>(R.id.et_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val cbTerms = findViewById<CheckBox>(R.id.cb_terms)

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // Local Validation
        if (name.isEmpty()) {
            findViewById<TextInputLayout>(R.id.til_name).error = getString(R.string.name_empty)
            etName.requestFocus()
            return
        }
        if (!ValidationUtils.isValidEmail(email)) {
            findViewById<TextInputLayout>(R.id.til_email).error = getString(R.string.invalid_email_format)
            etEmail.requestFocus()
            return
        }
        if (!ValidationUtils.isValidPassword(password)) {
            findViewById<TextInputLayout>(R.id.til_password).error = getString(R.string.password_too_short)
            etPassword.requestFocus()
            return
        }
        if (password != confirmPassword) {
            findViewById<TextInputLayout>(R.id.til_confirm_password).error = getString(R.string.passwords_dont_match)
            etConfirmPassword.requestFocus()
            return
        }
        if (!cbTerms.isChecked) {
            Toast.makeText(this, getString(R.string.signup_error), Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // Perform Firebase Signup
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid
                if (uid != null) {
                    com.enosh.fincalc.utils.UserUtils.ensureFinCalcUserProfile(this@SignupActivity, providedName = name, providedEmail = email)
                }

                // Save profile data (Off main thread)
                withContext(Dispatchers.IO) {
                    val securePref = SecurityUtils.getEncryptedPrefs(applicationContext)
                    securePref.edit {
                        putString("name", name)
                        putString("email", email)
                        putBoolean("has_account", true)
                        remove("password")
                    }
                }

                // Send verification email
                val user = auth.currentUser
                user?.sendEmailVerification()?.await()
                
                withContext(Dispatchers.Main) {
                    androidx.appcompat.app.AlertDialog.Builder(this@SignupActivity)
                        .setTitle("Verification Sent")
                        .setMessage(getString(R.string.verification_email_sent))
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                val message = when (e) {
                    is FirebaseAuthWeakPasswordException -> getString(R.string.password_too_short)
                    is FirebaseAuthInvalidCredentialsException -> getString(R.string.invalid_email_format)
                    is FirebaseAuthUserCollisionException -> "This email is already registered."
                    else -> e.localizedMessage ?: "Signup failed. Please try again."
                }
                Toast.makeText(this@SignupActivity, message, Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    private fun signInWithGoogle() {
        setLoading(true)
        val preferLegacy = getSharedPreferences("GooglePrefs", MODE_PRIVATE).getBoolean("PREFER_LEGACY", false)
        if (preferLegacy) {
            startLegacyGoogleSignIn()
            return
        }

        val credentialManager = try {
            CredentialManager.create(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Credential Manager Error", Toast.LENGTH_LONG).show()
            setLoading(false)
            return
        }
        
        val webClientId = try { getString(R.string.default_web_client_id) } catch (_: Exception) { "" }

        if (webClientId.isEmpty()) {
            Toast.makeText(this, "Configuration Error: Web Client ID missing.", Toast.LENGTH_LONG).show()
            setLoading(false)
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
                val result = withTimeoutOrNull(3000) {
                    credentialManager.getCredential(this@SignupActivity, request)
                }

                if (result == null) {
                    startLegacyGoogleSignIn()
                    return@launch
                }

                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", false) }
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    onAuthSuccess()
                } else {
                    Toast.makeText(this@SignupActivity, "Unexpected credential type", Toast.LENGTH_LONG).show()
                }
            } catch (_: GetCredentialCancellationException) {
                Toast.makeText(this@SignupActivity, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            } catch (e: NoCredentialException) {
                Toast.makeText(this@SignupActivity, "No Google accounts found.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("SignupActivity", "Google Sign-In error", e)
                startLegacyGoogleSignIn()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun onAuthSuccess() {
        val user = auth.currentUser
        SecurityUtils.skipNextLock = true
        SecurityUtils.hasAuthenticatedThisSession = true
        
        lifecycleScope.launch {
            com.enosh.fincalc.utils.UserUtils.ensureFinCalcUserProfile(this@SignupActivity)
            
            withContext(Dispatchers.IO) {
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit {
                    putBoolean("is_guest", false)
                    putString("email", user?.email)
                    putString("name", user?.displayName)
                    putString("profile_pic", user?.photoUrl?.toString())
                }
            }
            
            val isLockEnabled = withContext(Dispatchers.IO) { SecurityUtils.isAppLockEnabled(applicationContext) }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SignupActivity, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                if (isLockEnabled) {
                    val intent = Intent(this@SignupActivity, LockActivity::class.java)
                    intent.putExtra("DESTINATION", "HOME")
                    startActivity(intent)
                } else {
                    startActivity(Intent(this@SignupActivity, HomeActivity::class.java))
                }
                finish()
            }
        }
    }

    private fun startLegacyGoogleSignIn() {
        getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", true) }
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
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
                    ds.color = accentTeal
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        cbTerms.text = spannableString
        cbTerms.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }
}
