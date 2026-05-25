package com.example.visualvibefincal

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.visualvibefincal.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * FORGOT PASSWORD FLOW:
 * Uses Firebase Authentication password reset email.
 * 
 * DEVELOPER NOTE:
 * Ensure you have Email/Password auth enabled in Firebase Console.
 */
class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var layoutStep1: LinearLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var btnAction: MaterialButton
    private lateinit var tvStepTitle: TextView
    private lateinit var tvStepDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            auth = FirebaseAuth.getInstance()
            android.util.Log.d("FirebaseInit", "Firebase initialized successfully in ForgotPasswordActivity")
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase configuration error. Please check app setup.", Toast.LENGTH_LONG).show()
            android.util.Log.e("FirebaseInit", "Firebase initialization failed in ForgotPasswordActivity", e)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        layoutStep1 = findViewById(R.id.layout_step_1)
        etEmail = findViewById(R.id.et_email)
        tilEmail = findViewById(R.id.til_email)
        btnAction = findViewById(R.id.btn_action)
        tvStepTitle = findViewById(R.id.tv_step_title)
        tvStepDesc = findViewById(R.id.tv_step_desc)

        tvStepTitle.text = getString(R.string.reset_password)
        tvStepDesc.text = getString(R.string.enter_email_reset)
        btnAction.text = getString(R.string.send_reset_email)

        btnAction.setOnClickListener {
            handleResetAction()
        }
    }

    private fun handleResetAction() {
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.field_cannot_be_empty)
            return
        }
        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.error = getString(R.string.invalid_email_format)
            return
        }

        tilEmail.error = null
        btnAction.isEnabled = false
        btnAction.text = getString(R.string.sending)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                btnAction.isEnabled = true
                btnAction.text = getString(R.string.send_reset_email)
                
                if (task.isSuccessful) {
                    // Use Dialog for password reset message
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Check Your Email")
                        .setMessage(getString(R.string.reset_email_sent))
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()

                    android.util.Log.d("ForgotPassword", "Reset email sent")
                    
                    tvStepDesc.text = getString(R.string.reset_link_sent_to, email)
                    layoutStep1.visibility = View.GONE
                    btnAction.text = getString(R.string.back_to_login)
                    btnAction.setOnClickListener { finish() }
                } else {
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthInvalidUserException -> getString(R.string.no_account_found)
                        else -> exception?.localizedMessage ?: getString(R.string.reset_failed)
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    android.util.Log.e("ForgotPassword", "Firebase reset error", exception)
                }
            }
    }
}
