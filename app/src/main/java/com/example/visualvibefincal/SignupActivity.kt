package com.example.visualvibefincal

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
import com.example.visualvibefincal.utils.ValidationUtils
import com.example.visualvibefincal.utils.SecurityUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.graphics.toColorInt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

/**
 * SIGNUP FLOW UPDATE:
 * Now uses Firebase Authentication to create users.
 * 
 * DEVELOPER SETUP:
 * 1. Create a Firebase project at console.firebase.google.com
 * 2. Add this Android app (com.example.visualvibefincal) to the project.
 * 3. Download google-services.json and place it in the app/ folder.
 * 4. Enable Email/Password provider in Firebase Console -> Authentication -> Sign-in method.
 */
class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            auth = FirebaseAuth.getInstance()
            android.util.Log.d("FirebaseInit", "Firebase initialized successfully in SignupActivity")
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
                        
                        android.util.Log.d("SignupActivity", "Firebase user created successfully: $email")
                        
                        // Send verification email
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    Toast.makeText(this, getString(R.string.verification_email_sent), Toast.LENGTH_LONG).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
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

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
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
