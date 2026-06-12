package com.enosh.fincalc

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * LOGIN FLOW UPDATE:
 * Optimized for stability and performance.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val firebaseCredential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            lifecycleScope.launch {
                setLoading(true)
                try {
                    auth.signInWithCredential(firebaseCredential).await()
                    onAuthSuccess(account.email)
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Firebase Auth Failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false)
                }
            }
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                7 -> "Network Error."
                else -> "Google Sign-In Failed (${e.statusCode})"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            setLoading(false)
        }
    }

    private var isPinRecovery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isPinRecovery = intent.getBooleanExtra("is_pin_recovery", false)
        
        try {
            auth = FirebaseAuth.getInstance()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase setup error.", Toast.LENGTH_LONG).show()
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Pre-initialize off the main thread
        lifecycleScope.launch(Dispatchers.IO) {
            SecurityUtils.getEncryptedPrefs(applicationContext)
        }

        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
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
                putExtra(Intent.EXTRA_SUBJECT, "Help - Visual Vibe FinCal")
            }
            try { startActivity(Intent.createChooser(intent, "Send Email")) } catch (_: Exception) {}
        }

        etEmail.addTextChangedListener { findViewById<TextInputLayout>(R.id.til_email).error = null }
        etPassword.addTextChangedListener { findViewById<TextInputLayout>(R.id.til_password).error = null }

        btnLogin.setOnClickListener { performLogin() }
        btnGoogle.setOnClickListener { signInWithGoogle() }
        tvForgotPassword.setOnClickListener { startActivity(Intent(this, ForgotPasswordActivity::class.java)) }
        tvSignup.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }

        btnGuest.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit { putBoolean("is_guest", true) }
                }
                navigateAfterAuth(true)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        findViewById<MaterialButton>(R.id.btn_login).isEnabled = !isLoading
        findViewById<MaterialButton>(R.id.btn_google).isEnabled = !isLoading
        findViewById<ProgressBar>(R.id.progress_login)?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun performLogin() {
        val email = findViewById<TextInputEditText>(R.id.et_email).text.toString().trim()
        val password = findViewById<TextInputEditText>(R.id.et_password).text.toString()

        if (!ValidationUtils.isValidEmail(email)) {
            findViewById<TextInputLayout>(R.id.til_email).error = getString(R.string.invalid_email_format)
            return
        }
        if (password.isEmpty()) {
            findViewById<TextInputLayout>(R.id.til_password).error = getString(R.string.password_empty)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                if (user != null && !user.isEmailVerified) {
                    setLoading(false)
                    showResendVerificationDialog()
                    return@launch
                }
                onAuthSuccess(email)
            } catch (e: Exception) {
                val message = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found."
                    is FirebaseAuthInvalidCredentialsException -> getString(R.string.invalid_credentials)
                    else -> e.localizedMessage ?: "Login failed."
                }
                Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    private fun onAuthSuccess(email: String?) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                SecurityUtils.skipNextLock = true
                SecurityUtils.hasAuthenticatedThisSession = true
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit {
                    putBoolean("is_guest", false)
                    putString("email", email)
                    val user = FirebaseAuth.getInstance().currentUser
                    putString("name", user?.displayName)
                    putString("profile_pic", user?.photoUrl?.toString())
                }
            }
            
            if (isPinRecovery) {
                Toast.makeText(this@LoginActivity, "Authentication successful. Please reset your PIN in Settings.", Toast.LENGTH_LONG).show()
                val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                    putExtra("OPEN_SETTINGS", true)
                    putExtra("RESET_PIN", true)
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@LoginActivity, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                navigateAfterAuth(false)
            }
        }
    }

    private suspend fun navigateAfterAuth(isGuest: Boolean) {
        val isLockEnabled = withContext(Dispatchers.IO) {
            SecurityUtils.isAppLockEnabled(applicationContext)
        }
        if (isLockEnabled && !isGuest) {
            val intent = Intent(this, LockActivity::class.java)
            intent.putExtra("DESTINATION", "HOME")
            startActivity(intent)
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        finish()
    }

    private fun signInWithGoogle() {
        setLoading(true)
        val preferLegacy = getSharedPreferences("GooglePrefs", MODE_PRIVATE).getBoolean("PREFER_LEGACY", false)
        if (preferLegacy) {
            startLegacyGoogleSignIn()
            return
        }

        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@LoginActivity)
                val webClientId = getString(R.string.default_web_client_id)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

                val result = withTimeoutOrNull(3000) { credentialManager.getCredential(this@LoginActivity, request) }
                if (result == null) {
                    startLegacyGoogleSignIn()
                    return@launch
                }

                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                    onAuthSuccess(auth.currentUser?.email)
                }
            } catch (e: Exception) {
                startLegacyGoogleSignIn()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun startLegacyGoogleSignIn() {
        getSharedPreferences("GooglePrefs", MODE_PRIVATE).edit { putBoolean("PREFER_LEGACY", true) }
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun showResendVerificationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Email Not Verified")
            .setMessage(getString(R.string.verify_email_warning))
            .setPositiveButton(getString(R.string.resend_verification)) { _, _ ->
                auth.currentUser?.sendEmailVerification()
            }
            .setNegativeButton("OK", null)
            .show()
    }
}
