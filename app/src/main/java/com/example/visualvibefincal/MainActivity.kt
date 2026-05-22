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
        "Saving 10% monthly can grow significantly over time 📈",
        "Track your expenses to stay ahead 💰",
        "Compound interest is the eighth wonder of the world!",
        "Always keep an emergency fund of 3-6 months.",
        "Diversify your investments to manage risk.",
        "A healthy BMI is generally between 18.5 and 24.9 🍎"
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

        // Splash screen delay of 3 seconds
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
