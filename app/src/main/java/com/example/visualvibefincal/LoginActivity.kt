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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        fun validateInputs() {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            
            val isEmailValid = ValidationUtils.isValidEmail(email)
            val isPasswordValid = password.isNotEmpty()
            
            btnLogin.isEnabled = isEmailValid && isPasswordValid
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

            val securePref = SecurityUtils.getEncryptedPrefs(this)
            val savedEmail = securePref.getString("email", null)
            val savedPassword = securePref.getString("password", null)

            if (email == savedEmail && password == savedPassword && !email.isNullOrEmpty()) {
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
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show()
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
}
