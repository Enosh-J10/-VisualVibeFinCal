package com.example.visualvibefincal

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
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

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        setupTermsClickable()

        fun validateInputs() {
            // We keep the button enabled to allow showing error messages on click
            // but we can still use this to clear errors or perform live checks if needed
        }

        etName.addTextChangedListener { 
            tilName.error = null
        }
        etEmail.addTextChangedListener { 
            tilEmail.error = null
        }
        etPassword.addTextChangedListener { 
            tilPassword.error = null
        }
        etConfirmPassword.addTextChangedListener { 
            tilConfirmPassword.error = null
        }

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

            val securePref = SecurityUtils.getEncryptedPrefs(this)
            securePref.edit {
                putString("name", name)
                putString("email", email)
                putString("password", password)
                // Mark that we have a saved account
                putBoolean("has_account", true)
            }
            
            android.util.Log.d("SignupActivity", "User record saved successfully for: $email")
            
            Toast.makeText(this, getString(R.string.signup_successful), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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
        val endIndex = startIndex + termsAndConditions.length

        if (startIndex != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@SignupActivity, TermsActivity::class.java))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = "#00D1B2".toColorInt() // Match accent_teal
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        cbTerms.text = spannableString
        cbTerms.setOnTouchListener { v, event ->
            val checkbox = v as CheckBox
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val text = checkbox.text as Spanned
                val x = event.x.toInt() - checkbox.totalPaddingLeft + checkbox.scrollX
                val y = event.y.toInt() - checkbox.totalPaddingTop + checkbox.scrollY
                
                val layout = checkbox.layout
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                
                val spans = text.getSpans(off, off, ClickableSpan::class.java)
                if (spans.isNotEmpty()) {
                    spans[0].onClick(checkbox)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }
}
