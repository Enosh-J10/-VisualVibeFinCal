package com.enosh.fincalc.utils

import android.content.Context
import com.enosh.fincalc.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

object UserUtils {
    const val PREFS_NAME = "UserPrefs"

    // Session-only keys (cleared on logout)
    private val SESSION_KEYS = listOf(
        "is_guest",
        "keep_me_signed_in",
        "email",
        "name",
        "profile_pic"
    )

    fun getScopedKey(uid: String, key: String) = "${key}_$uid"
    
    fun getEffectiveUid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isGuest = prefs.getBoolean("is_guest", false)
        if (isGuest) return "guest"
        return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    }

    fun getFinCalcIdKey(uid: String) = getScopedKey(uid, "finCalcId")

    fun generateStableFinCalcId(uid: String): String {
        return try {
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(uid.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .uppercase()
            "FIN-" + hash.take(6)
        } catch (e: Exception) {
            "FIN-" + uid.take(6).uppercase()
        }
    }

    fun logout(context: Context, onComplete: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isGuest = sharedPref.getBoolean("is_guest", false)
        
        auth.signOut()
        
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.enosh.fincalc.R.string.default_web_client_id))
            .requestEmail()
            .build()
            
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
            if (isGuest) {
                // Nuclear clear for guest mode
                sharedPref.edit().clear().apply()
                context.getSharedPreferences("AssistantPrefs_guest", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("NotesPrefs_guest", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("BudgetPrefs_guest", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("ToolPrefs_guest", Context.MODE_PRIVATE).edit().clear().apply()
                
                // Clear Database
                @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        com.enosh.fincalc.data.local.AppDatabase.getDatabase(context).clearAllTables()
                    } catch (e: Exception) {
                        android.util.Log.e("UserUtils", "DB clear failed", e)
                    }
                }
                
                // Clear Cache & Temp Files
                try {
                    context.cacheDir.deleteRecursively()
                    context.filesDir.deleteRecursively()
                } catch (e: Exception) {
                    android.util.Log.e("UserUtils", "File clear failed", e)
                }
            } else {
                sharedPref.edit().apply {
                    SESSION_KEYS.forEach { remove(it) }
                }.apply()
            }
            
            SecurityUtils.hasAuthenticatedThisSession = false
            SecurityUtils.skipNextLock = false
            com.enosh.fincalc.data.local.AppDatabase.resetInstance()
            onComplete()
        }
    }

    suspend fun ensureFinCalcUserProfile(
        context: Context,
        providedName: String? = null,
        providedEmail: String? = null,
        profilePicUri: android.net.Uri? = null
    ) {
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        if (uid.isBlank()) return
        
        val finCalcId = generateStableFinCalcId(uid)
        
        android.util.Log.d("DP_UPLOAD", "ensureFinCalcUserProfile: uid=$uid")
        
        var finalProfilePicUrl: String? = null
        if (profilePicUri != null) {
            try {
                android.util.Log.d("DP_UPLOAD", "uri=$profilePicUri")
                
                // 1. Create local directory for profile pictures
                val dir = java.io.File(context.filesDir, "profile_pictures")
                if (!dir.exists()) dir.mkdirs()
                
                // 2. Target persistent local file
                val file = java.io.File(dir, "profile_$uid.jpg")
                
                // 3. Copy image from content Uri to local file
                context.contentResolver.openInputStream(profilePicUri)?.use { input ->
                    java.io.FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                if (file.exists()) {
                    finalProfilePicUrl = android.net.Uri.fromFile(file).toString()
                    android.util.Log.d("DP_UPLOAD", "localPath=$finalProfilePicUrl")
                    android.util.Log.d("DP_UPLOAD", "copy success")
                    
                    // Save locally for instant access
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putString(getScopedKey(uid, "profile_pic_local"), finalProfilePicUrl)
                        .apply()
                } else {
                    throw Exception("Failed to create local persistent profile file")
                }
            } catch (e: Exception) {
                android.util.Log.e("DP_UPLOAD", "Local copy failed: ${e.message}")
                throw e
            }
        }

        uploadCurrentUser(providedName, providedEmail, finalProfilePicUrl)
        updateFcmToken()
        
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(getFinCalcIdKey(uid), finCalcId)
            .apply()
    }

    suspend fun uploadCurrentUser(
        providedName: String? = null, 
        providedEmail: String? = null,
        profilePicUrl: String? = null
    ) {
        android.util.Log.d("UserUtils", "uploadCurrentUser: name=$providedName, hasPic=${profilePicUrl != null}")
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        val initialName = firebaseUser.displayName ?: ""
        val name = when {
            providedName != null && providedName.isNotBlank() -> providedName
            initialName.isNotBlank() -> initialName
            else -> "User"
        }
        val email = providedEmail ?: firebaseUser.email ?: ""
        val finCalcId = generateStableFinCalcId(uid)
        val photoUrl = profilePicUrl ?: firebaseUser.photoUrl?.toString()

        // Update FirebaseAuth profile as well
        if ((providedName != null && providedName.isNotBlank()) || profilePicUrl != null) {
            android.util.Log.d("UserUtils", "Updating FirebaseUser profile...")
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                if (providedName != null && providedName.isNotBlank()) displayName = providedName
                if (profilePicUrl != null) photoUri = android.net.Uri.parse(profilePicUrl)
            }
            try {
                firebaseUser.updateProfile(profileUpdates).await()
                android.util.Log.d("UserUtils", "FirebaseUser profile updated, reloading...")
                // IMPORTANT: Reload the user to refresh the local cache/token
                firebaseUser.reload().await()
                android.util.Log.d("UserUtils", "FirebaseUser reloaded")
            } catch (e: Exception) {
                android.util.Log.e("UserUtils", "FirebaseAuth profile update failed", e)
            }
        }

        val userMap = mutableMapOf<String, Any>(
            "uid" to uid,
            "email" to email,
            "searchableEmail" to email.lowercase().trim(),
            "name" to name,
            "searchableName" to name.lowercase().trim(),
            "finCalcId" to finCalcId.uppercase().trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        
        if (photoUrl != null) {
            userMap["profilePictureUrl"] = photoUrl
        }

        try {
            android.util.Log.d("UserUtils", "Updating Firestore user document for $uid...")
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .await()
            android.util.Log.d("UserUtils", "Firestore updated successfully with url: ${userMap["profilePictureUrl"]}")
        } catch (e: Exception) {
            android.util.Log.e("UserUtils", "Failed to upload user profile: ${e.message}")
        }
    }

    suspend fun updateFcmToken() {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val token = FirebaseMessaging.getInstance().token.await()
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token).await()
        } catch (e: Exception) {
            android.util.Log.e("UserUtils", "Failed to update FCM token", e)
        }
    }


    suspend fun removeProfilePicture(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        if (uid.isBlank()) return

        try {
            // 1. Clear SharedPreferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .remove(getScopedKey(uid, "profile_pic_local"))
                .apply()

            // 2. Clear from Firestore
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("profilePictureUrl", null)
                .await()

            // 3. Clear from FirebaseAuth (optional but good for consistency)
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                photoUri = null
            }
            firebaseUser.updateProfile(profileUpdates).await()
            firebaseUser.reload().await()

            // 4. Optionally delete the local file
            val dir = java.io.File(context.filesDir, "profile_pictures")
            val file = java.io.File(dir, "profile_$uid.jpg")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("UserUtils", "Failed to remove profile picture", e)
            throw e
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        val db = FirebaseFirestore.getInstance()
        val results = mutableListOf<User>()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return emptyList()

        val lowerQuery = trimmedQuery.lowercase()
        val upperQuery = trimmedQuery.uppercase()

        try {
            // 1. Email search (Exact match)
            val emailSnap = db.collection("users")
                .whereEqualTo("searchableEmail", lowerQuery)
                .get()
                .await()
            results.addAll(emailSnap.documents.mapNotNull { it.toObject(User::class.java) })

            // 2. FinCalc ID search (Exact match)
            val idSnap = db.collection("users")
                .whereEqualTo("finCalcId", upperQuery)
                .get()
                .await()
            results.addAll(idSnap.documents.mapNotNull { it.toObject(User::class.java) })

            // 3. Name prefix search
            val nameSnap = db.collection("users")
                .whereGreaterThanOrEqualTo("searchableName", lowerQuery)
                .whereLessThanOrEqualTo("searchableName", lowerQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()
            results.addAll(nameSnap.documents.mapNotNull { it.toObject(User::class.java) })

        } catch (e: Exception) {
            android.util.Log.e("UserUtils", "Search failed: query=$trimmedQuery", e)
        }

        return results.distinctBy { it.uid }
    }
}
