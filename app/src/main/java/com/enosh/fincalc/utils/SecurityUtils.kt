package com.enosh.fincalc.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityUtils {
    private const val ENCRYPTED_PREFS_NAME = "secure_user_prefs"
    private var sharedPrefs: SharedPreferences? = null

    // Session-based flags
    var hasAuthenticatedThisSession = false
    var skipNextLock = false

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
            } catch (e: Throwable) {
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

    private fun getEffectiveUid(context: Context): String {
        return UserUtils.getEffectiveUid(context)
    }

    fun isAppLockEnabled(context: Context): Boolean {
        val uid = getEffectiveUid(context)
        return getEncryptedPrefs(context).getBoolean("app_lock_enabled_$uid", false)
    }

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        val uid = getEffectiveUid(context)
        getEncryptedPrefs(context).edit().putBoolean("app_lock_enabled_$uid", enabled).apply()
    }

    fun getAppPin(context: Context): String? {
        val uid = getEffectiveUid(context)
        return getEncryptedPrefs(context).getString("app_pin_$uid", null)
    }

    fun setAppPin(context: Context, pin: String) {
        val uid = getEffectiveUid(context)
        // Hash the PIN before saving for better security
        val hashedPin = hashPin(pin)
        getEncryptedPrefs(context).edit().putString("app_pin_$uid", hashedPin).apply()
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val savedPin = getAppPin(context) ?: return false
        // Migration: If saved PIN is exactly 4 chars, it might be an old unhashed PIN.
        // SHA-256 hashes are always 64 characters long.
        if (savedPin.length == 4 && savedPin == inputPin) {
            setAppPin(context, inputPin) // Upgrade to hash
            return true
        }
        return savedPin == hashPin(inputPin)
    }

    private fun hashPin(pin: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin // Fallback to plain text if hashing fails (should not happen)
        }
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val uid = getEffectiveUid(context)
        return getEncryptedPrefs(context).getBoolean("biometric_enabled_$uid", false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        val uid = getEffectiveUid(context)
        getEncryptedPrefs(context).edit().putBoolean("biometric_enabled_$uid", enabled).apply()
    }
}
