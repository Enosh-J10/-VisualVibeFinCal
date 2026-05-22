package com.example.visualvibefincal.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val ENCRYPTED_PREFS_NAME = "secure_user_prefs"
    private var sharedPrefs: SharedPreferences? = null

    @Synchronized
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        if (sharedPrefs == null) {
            try {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                sharedPrefs = EncryptedSharedPreferences.create(
                    context.applicationContext,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                android.util.Log.e("SecurityUtils", "Failed to create EncryptedSharedPreferences", e)
                // Fallback to regular SharedPreferences if encryption fails
                sharedPrefs = context.applicationContext.getSharedPreferences(
                    ENCRYPTED_PREFS_NAME + "_fallback",
                    Context.MODE_PRIVATE
                )
            }
        }
        return sharedPrefs!!
    }

    fun isAppLockEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean("app_lock_enabled", false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean("app_lock_enabled", enabled).apply()
    }

    fun getAppPin(context: Context): String? {
        return getEncryptedPrefs(context).getString("app_pin", null)
    }

    fun setAppPin(context: Context, pin: String) {
        getEncryptedPrefs(context).edit().putString("app_pin", pin).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean("biometric_enabled", false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean("biometric_enabled", enabled).apply()
    }
}