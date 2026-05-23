package com.example.visualvibefincal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.visualvibefincal.utils.SecurityUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etNewPassword = findViewById<TextInputEditText>(R.id.et_new_password)
        val btnReset = findViewById<MaterialButton>(R.id.btn_reset)

        btnReset.setOnClickListener {
            val email = etEmail.text.toString()
            val newPassword = etNewPassword.text.toString()

            if (email.isNotEmpty() && newPassword.isNotEmpty()) {
                val securePref = SecurityUtils.getEncryptedPrefs(this)
                val savedEmail = securePref.getString("email", null)

                if (email == savedEmail) {
                    securePref.edit {
                        putString("password", newPassword)
                    }
                    Toast.makeText(this, getString(R.string.password_updated), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.email_not_found), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
