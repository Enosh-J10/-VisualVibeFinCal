package com.example.visualvibefincal

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.visualvibefincal.utils.SecurityUtils
import com.example.visualvibefincal.utils.ValidationUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Interface for password reset operations. 
 * In a production app, these would be API calls to a secure backend.
 */
interface AuthRepository {
    fun sendPasswordResetOtp(email: String, onResult: (Boolean) -> Unit)
    fun verifyOtp(email: String, otp: String, onResult: (Boolean) -> Unit)
}

class ForgotPasswordActivity : AppCompatActivity() {

    private var currentStep = 1
    private var resendCooldownActive = false
    
    // Placeholder repository. TODO: Replace with real production implementation (e.g. Firebase or custom API)
    private val authRepository: AuthRepository = object : AuthRepository {
        override fun sendPasswordResetOtp(email: String, onResult: (Boolean) -> Unit) {
            // TODO: Call backend API to send email. 
            // DO NOT generate or expose OTP here in production.
            android.util.Log.i("Auth", "Requesting OTP for $email from backend...")
            onResult(true) 
        }

        override fun verifyOtp(email: String, otp: String, onResult: (Boolean) -> Unit) {
            // TODO: Call backend API to verify OTP.
            // For placeholder/debug testing, we accept '123456'
            onResult(otp == "123456")
        }
    }

    private lateinit var layoutStep1: LinearLayout
    private lateinit var layoutStep2: LinearLayout
    private lateinit var layoutStep3: LinearLayout

    private lateinit var etEmail: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etOtp: TextInputEditText
    private lateinit var tilOtp: TextInputLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var btnAction: MaterialButton
    private lateinit var tvResendOtp: TextView
    private lateinit var tvStepTitle: TextView
    private lateinit var tvStepDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // Initialize views
        layoutStep1 = findViewById(R.id.layout_step_1)
        layoutStep2 = findViewById(R.id.layout_step_2)
        layoutStep3 = findViewById(R.id.layout_step_3)

        etEmail = findViewById(R.id.et_email)
        tilEmail = findViewById(R.id.til_email)
        etOtp = findViewById(R.id.et_otp)
        tilOtp = findViewById(R.id.til_otp)
        etNewPassword = findViewById(R.id.et_new_password)
        tilNewPassword = findViewById(R.id.til_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)

        btnAction = findViewById(R.id.btn_action)
        tvResendOtp = findViewById(R.id.tv_resend_otp)
        tvStepTitle = findViewById(R.id.tv_step_title)
        tvStepDesc = findViewById(R.id.tv_step_desc)

        updateStepUi()

        btnAction.setOnClickListener {
            handleAction()
        }

        tvResendOtp.setOnClickListener {
            if (!resendCooldownActive) {
                requestOtp()
            }
        }
    }

    private fun updateStepUi() {
        layoutStep1.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        layoutStep2.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        layoutStep3.visibility = if (currentStep == 3) View.VISIBLE else View.GONE

        when (currentStep) {
            1 -> {
                tvStepTitle.text = getString(R.string.forgot_password).removeSuffix("?")
                tvStepDesc.text = getString(R.string.dont_have_account).split("?").last() // Placeholder or better use dedicated string
                tvStepDesc.text = "Enter your registered email to receive an OTP."
                btnAction.text = "Send OTP"
                tvResendOtp.visibility = View.GONE
            }
            2 -> {
                tvStepTitle.text = getString(R.string.verify_otp)
                tvStepDesc.text = getString(R.string.enter_otp)
                btnAction.text = getString(R.string.verify_otp)
                tvResendOtp.visibility = View.VISIBLE
            }
            3 -> {
                tvStepTitle.text = getString(R.string.reset_password)
                tvStepDesc.text = "Create a strong new password for your account."
                btnAction.text = getString(R.string.reset_password)
                tvResendOtp.visibility = View.GONE
            }
        }
    }

    private fun handleAction() {
        when (currentStep) {
            1 -> {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) {
                    tilEmail.error = getString(R.string.field_cannot_be_empty)
                    return
                }
                if (!ValidationUtils.isValidEmail(email)) {
                    tilEmail.error = getString(R.string.invalid_email_format)
                    return
                }

                val securePref = SecurityUtils.getEncryptedPrefs(this)
                val savedEmail = securePref.getString("email", null)

                if (email == savedEmail) {
                    requestOtp()
                    currentStep = 2
                    updateStepUi()
                } else {
                    tilEmail.error = getString(R.string.email_not_found)
                }
            }
            2 -> {
                val inputOtp = etOtp.text.toString().trim()
                if (inputOtp.isEmpty()) {
                    tilOtp.error = getString(R.string.field_cannot_be_empty)
                    return
                }
                
                authRepository.verifyOtp(etEmail.text.toString(), inputOtp) { success ->
                    if (success) {
                        currentStep = 3
                        updateStepUi()
                    } else {
                        tilOtp.error = "Invalid OTP"
                    }
                }
            }
            3 -> {
                val newPass = etNewPassword.text.toString()
                val confirmPass = etConfirmPassword.text.toString()

                var hasError = false
                if (newPass.isEmpty()) {
                    tilNewPassword.error = getString(R.string.password_empty)
                    hasError = true
                } else if (!ValidationUtils.isValidPassword(newPass)) {
                    tilNewPassword.error = getString(R.string.password_too_short)
                    hasError = true
                }

                if (confirmPass != newPass) {
                    tilConfirmPassword.error = getString(R.string.passwords_dont_match)
                    hasError = true
                }

                if (hasError) return

                val securePref = SecurityUtils.getEncryptedPrefs(this)
                securePref.edit {
                    putString("password", newPass)
                }
                
                Toast.makeText(this, getString(R.string.password_updated), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun requestOtp() {
        val email = etEmail.text.toString().trim()
        authRepository.sendPasswordResetOtp(email) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.otp_sent), Toast.LENGTH_LONG).show()
                startResendCooldown()
            } else {
                Toast.makeText(this, "Failed to send OTP. Try again later.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startResendCooldown() {
        resendCooldownActive = true
        tvResendOtp.isEnabled = false
        
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResendOtp.text = "Resend OTP in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                resendCooldownActive = false
                tvResendOtp.isEnabled = true
                tvResendOtp.text = "Resend OTP"
            }
        }.start()
    }
}
