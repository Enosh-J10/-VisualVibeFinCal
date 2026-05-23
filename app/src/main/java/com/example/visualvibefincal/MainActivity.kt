package com.example.visualvibefincal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import android.widget.TextView
import com.example.visualvibefincal.utils.SecurityUtils
import com.example.visualvibefincal.utils.NotificationHelper

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
        
        val updateTipRunnable = object : Runnable {
            override fun run() {
                tvTip.text = tips.random()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTipRunnable)

        // Wait 3 seconds then go to login or lock screen
        handler.postDelayed({
            handler.removeCallbacks(updateTipRunnable)
            if (SecurityUtils.isAppLockEnabled(this)) {
                val intent = Intent(this, LockActivity::class.java)
                intent.putExtra("DESTINATION", "LOGIN")
                startActivity(intent)
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            finish()
        }, 3000)
    }
}
