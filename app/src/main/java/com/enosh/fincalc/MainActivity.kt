package com.enosh.fincalc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import android.widget.TextView
import com.enosh.fincalc.utils.SecurityUtils
import com.enosh.fincalc.utils.NotificationHelper

class MainActivity : AppCompatActivity() {
    private val tips = listOf(
        "Try to save a bit of your money every month! 📈",
        "Track your spending to see where your money goes 💰",
        "The earlier you start saving, the more it grows! 🪙",
        "It's good to keep some cash for emergencies. 🏦",
        "Don't put all your eggs in one basket when investing. 🥚",
        "Staying active helps keep your BMI in a healthy range 🍎"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvTip = findViewById<TextView>(R.id.tv_tagline)
        val handler = Handler(Looper.getMainLooper())
        
        fun showNextTip() {
            tvTip.animate().alpha(0f).setDuration(200).withEndAction {
                tvTip.text = tips.random()
                tvTip.animate().alpha(1f).setDuration(200).start()
            }.start()
        }

        val updateTipRunnable = object : Runnable {
            override fun run() {
                showNextTip()
                handler.postDelayed(this, 3000) 
            }
        }
        handler.post(updateTipRunnable)

        tvTip.setOnClickListener {
            handler.removeCallbacks(updateTipRunnable)
            showNextTip()
            handler.postDelayed(updateTipRunnable, 3000)
        }

        // Wait 3 seconds then go to login or home
        handler.postDelayed({
            handler.removeCallbacks(updateTipRunnable)
            
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val isGuest = getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("is_guest", false)
            val keepMeSignedIn = getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("keep_me_signed_in", true)

            if ((auth.currentUser != null || isGuest) && keepMeSignedIn) {
                val intent = if (SecurityUtils.isAppLockEnabled(this)) {
                    Intent(this, LockActivity::class.java).apply {
                        putExtra("DESTINATION", "HOME")
                    }
                } else {
                    Intent(this, HomeActivity::class.java)
                }
                
                // Pass deep link data if any
                getIntent().data?.let { uri ->
                    intent.data = uri
                }
                
                startActivity(intent)
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 3000)
    }
}
