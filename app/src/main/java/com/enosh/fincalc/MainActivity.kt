package com.enosh.fincalc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import android.widget.TextView
import com.enosh.fincalc.utils.SecurityUtils
import com.enosh.fincalc.utils.UserUtils

class MainActivity : AppCompatActivity() {
    private val tips = listOf(
        "Try to save a bit of your money every month! 📈",
        "Track your spending to see where your money goes 💰",
        "The earlier you start saving, the more it grows! 🪙",
        "It's good to keep some cash for emergencies. 🏦",
        "Don't put all your eggs in one basket when investing. 🥚",
        "Staying active helps keep your BMI in a healthy range 🍎",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvTip = findViewById<TextView>(R.id.tv_tagline)
        val handler = Handler(Looper.getMainLooper())
        
        fun showNextTip() {
            if (isFinishing || isDestroyed) return
            tvTip?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
                if (isFinishing || isDestroyed) return@withEndAction
                tvTip.text = tips.random()
                tvTip.animate()?.alpha(1f)?.setDuration(200)?.start()
            }?.start()
        }

        val updateTipRunnable = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return
                showNextTip()
                handler.postDelayed(this, 3000) 
            }
        }
        handler.post(updateTipRunnable)

        tvTip?.setOnClickListener {
            handler.removeCallbacks(updateTipRunnable)
            showNextTip()
            handler.postDelayed(updateTipRunnable, 3000)
        }

        // Wait 3 seconds then go to login or home
        handler.postDelayed({
            handler.removeCallbacks(updateTipRunnable)
            
            try {
                val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (e: Throwable) { null }
                val sharedPref = getSharedPreferences(UserUtils.PREFS_NAME, MODE_PRIVATE)
                val isGuest = sharedPref.getBoolean("is_guest", false)
                val keepMeSignedIn = sharedPref.getBoolean("keep_me_signed_in", true)

                if (auth != null && (auth.currentUser != null || isGuest) && keepMeSignedIn) {
                    val isLockEnabled = try { SecurityUtils.isAppLockEnabled(this) } catch (e: Throwable) { false }
                    val intent = if (isLockEnabled) {
                        Intent(this, LockActivity::class.java).apply {
                            putExtra("DESTINATION", "HOME")
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                    
                    // Pass deep link data if any
                    intent.data = getIntent().data
                    
                    startActivity(intent)
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Startup failure", e)
                try {
                    startActivity(Intent(this, LoginActivity::class.java))
                } catch (internal: Exception) {
                    // Critical failure
                }
            }
            finish()
        }, 3000)
    }
}
