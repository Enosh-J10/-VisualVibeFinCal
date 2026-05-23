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
import kotlin.random.Random

class ForgotPasswordActivity : AppCompatActivity() {

    private var currentStep = 1
    private var generatedOtp: String? = null
    private var otpExpiryTime: Long = 0
    private var resendCooldownActive = false

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
                sendOtp()
            }
        }
    }

    private fun updateStepUi() {
        layoutStep1.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        layoutStep2.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        layoutStep3.visibility = if (currentStep == 3) View.VISIBLE else View.GONE

        when (currentStep) {
            1 -> {
                tvStepTitle.text = "Forgot Password"
                tvStepDesc.text = "Enter your registered email to receive an OTP."
                btnAction.text = "Send OTP"
                tvResendOtp.visibility = View.GONE
            }
            2 -> {
                tvStepTitle.text = "Verify OTP"
                tvStepDesc.text = "Enter the 6-digit code sent to ${etEmail.text}."
                btnAction.text = "Verify OTP"
                tvResendOtp.visibility = View.VISIBLE
            }
            3 -> {
                tvStepTitle.text = "Reset Password"
                tvStepDesc.text = "Create a strong new password for your account."
                btnAction.text = "Reset Password"
                tvResendOtp.visibility = View.GONE
            }
        }
    }

    private fun handleAction() {
        when (currentStep) {
            1 -> {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) {
                    tilEmail.error = "Email is required"
                    return
                }
                if (!ValidationUtils.isValidEmail(email)) {
                    tilEmail.error = "Invalid email format"
                    return
                }

                val securePref = SecurityUtils.getEncryptedPrefs(this)
                val savedEmail = securePref.getString("email", null)

                if (email == savedEmail) {
                    sendOtp()
                    currentStep = 2
                    updateStepUi()
                } else {
                    // Safety: In real apps, don't always expose if email exists. 
                    // But for this local app requirements, we show error.
                    tilEmail.error = "Account with this email not found"
                }
            }
            2 -> {
                val inputOtp = etOtp.text.toString().trim()
                if (inputOtp.isEmpty()) {
                    tilOtp.error = "OTP is required"
                    return
                }
                
                if (System.currentTimeMillis() > otpExpiryTime) {
                    tilOtp.error = "OTP has expired. Please resend."
                    return
                }

                if (inputOtp == generatedOtp) {
                    currentStep = 3
                    updateStepUi()
                } else {
                    tilOtp.error = "Invalid OTP"
                }
            }
            3 -> {
                val newPass = etNewPassword.text.toString()
                val confirmPass = etConfirmPassword.text.toString()

                var hasError = false
                if (newPass.isEmpty()) {
                    tilNewPassword.error = "Password required"
                    hasError = true
                } else if (!ValidationUtils.isValidPassword(newPass)) {
                    tilNewPassword.error = "Password too weak"
                    hasError = true
                }

                if (confirmPass != newPass) {
                    tilConfirmPassword.error = "Passwords do not match"
                    hasError = true
                }

                if (hasError) return

                val securePref = SecurityUtils.getEncryptedPrefs(this)
                securePref.edit {
                    putString("password", newPass)
                }
                
                Toast.makeText(this, "Password reset successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun sendOtp() {
        // Generate 6-digit OTP
        generatedOtp = (100000 + Random.nextInt(900000)).toString()
        // Expire in 5 minutes
        otpExpiryTime = System.currentTimeMillis() + (5 * 60 * 1000)
        
        // In a real app, this would be an API call to send an email.
        // For this task, we'll log it and show a Toast with the code for testing purposes.
        android.util.Log.d("ForgotPassword", "Generated OTP for ${etEmail.text}: $generatedOtp")
        Toast.makeText(this, "OTP sent to ${etEmail.text}: $generatedOtp", Toast.LENGTH_LONG).show()
        
        startResendCooldown()
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
