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
import com.example.visualvibefincal.utils.ValidationUtils
import com.example.visualvibefincal.utils.SecurityUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

/**
 * LOGIN FLOW UPDATE:
 * Now uses Firebase Authentication to sign in users.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            auth = FirebaseAuth.getInstance()
            android.util.Log.d("FirebaseInit", "Firebase initialized successfully in LoginActivity")
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
                            Toast.makeText(this, getString(R.string.verify_email_warning), Toast.LENGTH_LONG).show()
                            
                            // Show resend button or dialog
                            showResendVerificationDialog()
                            return@addOnCompleteListener
                        }

                        android.util.Log.d("LoginActivity", "Firebase login successful: $email")
                        SecurityUtils.skipNextLock = true
                        SecurityUtils.hasAuthenticatedThisSession = true
                        
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit {
                            putBoolean("is_guest", false)
                            putString("email", email)
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
