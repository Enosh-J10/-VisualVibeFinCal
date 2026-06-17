package com.enosh.fincalc.utils

import android.content.Context
import com.enosh.fincalc.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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
        
        auth.signOut()
        
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.enosh.fincalc.R.string.default_web_client_id))
            .requestEmail()
            .build()
            
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
            sharedPref.edit().apply {
                SESSION_KEYS.forEach { remove(it) }
            }.apply()
            
            SecurityUtils.hasAuthenticatedThisSession = false
            SecurityUtils.skipNextLock = false
            onComplete()
        }
    }

    suspend fun ensureFinCalcUserProfile(
        context: Context,
        providedName: String? = null,
        providedEmail: String? = null
    ) {
        uploadCurrentUser(providedName, providedEmail)
        
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        val finCalcId = generateStableFinCalcId(uid)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(getFinCalcIdKey(uid), finCalcId)
            .apply()
    }

    suspend fun uploadCurrentUser(providedName: String? = null, providedEmail: String? = null) {
        val auth = FirebaseAuth.getInstance()
        val firebaseUser = auth.currentUser ?: return
        val uid = firebaseUser.uid
        val name = providedName ?: firebaseUser.displayName ?: "User"
        val email = providedEmail ?: firebaseUser.email ?: ""
        val finCalcId = generateStableFinCalcId(uid)

        val userMap = mapOf(
            "uid" to uid,
            "email" to email,
            "searchableEmail" to email.lowercase().trim(),
            "name" to name,
            "searchableName" to name.lowercase().trim(),
            "finCalcId" to finCalcId.uppercase().trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(userMap)
            .await()
    }


    suspend fun searchUsers(query: String): List<User> {
        val db = FirebaseFirestore.getInstance()
        val results = mutableListOf<User>()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return emptyList()

        // Email search
        val emailSnap = db.collection("users")
            .whereEqualTo("searchableEmail", trimmedQuery.lowercase())
            .get()
            .await()
        results.addAll(emailSnap.documents.mapNotNull { it.toObject(User::class.java) })

        // FinCalc ID search
        val idSnap = db.collection("users")
            .whereEqualTo("finCalcId", trimmedQuery.uppercase())
            .get()
            .await()
        results.addAll(idSnap.documents.mapNotNull { it.toObject(User::class.java) })

        // Name search
        val nameSnap = db.collection("users")
            .orderBy("searchableName")
            .startAt(trimmedQuery.lowercase())
            .endAt(trimmedQuery.lowercase() + "\uf8ff")
            .get()
            .await()
        results.addAll(nameSnap.documents.mapNotNull { it.toObject(User::class.java) })

        return results.distinctBy { it.uid }
    }
}
