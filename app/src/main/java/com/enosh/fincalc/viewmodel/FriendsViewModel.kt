package com.enosh.fincalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.enosh.fincalc.data.model.User
import com.enosh.fincalc.data.model.FriendRequest
import com.enosh.fincalc.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var pendingListener: ListenerRegistration? = null
    private var sentListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null

    init {
        fetchPendingRequests()
        fetchFriends()
    }

    override fun onCleared() {
        super.onCleared()
        pendingListener?.remove()
        sentListener?.remove()
        friendsListener?.remove()
    }

    fun searchUsers(query: String) {
        if (query.trim().isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val currentUid = auth.currentUser?.uid
                val users = UserUtils.searchUsers(query)
                _searchResults.value = users.filter { it.uid != currentUid }
            } catch (e: Exception) {
                Log.e("FriendsError", "Search failed", e)
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun sendFriendRequest(toUser: User) {
        val currentUserAuth = auth.currentUser ?: run {
            _errorMessage.value = "Please log in to add friends."
            return
        }
        val fromUid = currentUserAuth.uid
        val toUid = toUser.uid

        if (fromUid == toUid) {
            _errorMessage.value = "You cannot add yourself."
            return
        }

        viewModelScope.launch {
            try {
                // Fetch current user profile from Firestore
                val currentUserDoc = db.collection("users").document(fromUid).get().await()
                if (!currentUserDoc.exists()) {
                    _errorMessage.value = "Profile is still syncing. Please try again."
                    return@launch
                }
                
                val fromName = currentUserDoc.getString("name") ?: "User"
                val fromEmail = currentUserDoc.getString("email") ?: ""
                val fromFinCalcId = currentUserDoc.getString("finCalcId") ?: ""

                if (fromFinCalcId.isBlank()) {
                    _errorMessage.value = "Profile is incomplete. Please try again."
                    return@launch
                }

                val requestId = "${fromUid}_$toUid"
                val reverseRequestId = "${toUid}_$fromUid"

                // Check if already friends using deterministic ID
                val uids = listOf(fromUid, toUid).sorted()
                val friendshipId = "${uids[0]}_${uids[1]}"
                
                val friendDoc = db.collection("friends").document(friendshipId).get().await()
                if (friendDoc.exists()) {
                    _errorMessage.value = "You are already friends."
                    return@launch
                }

                // Check if request already exists (either direction)
                val existingOut = db.collection("friendRequests").document(requestId).get().await()
                val existingIn = db.collection("friendRequests").document(reverseRequestId).get().await()
                
                if (existingOut.exists()) {
                    val status = existingOut.getString("status") ?: "pending"
                    if (status == "pending") {
                        _errorMessage.value = "Request already sent."
                        return@launch
                    } else if (status == "accepted") {
                        // Auto-repair if friendship doc missing but request accepted
                        repairFriendship(fromUid, toUid)
                        _errorMessage.value = "You are already friends."
                        return@launch
                    }
                }
                
                if (existingIn.exists()) {
                    val status = existingIn.getString("status") ?: "pending"
                    if (status == "pending") {
                        _errorMessage.value = "You have a pending request from this user."
                        return@launch
                    } else if (status == "accepted") {
                        repairFriendship(fromUid, toUid)
                        _errorMessage.value = "You are already friends."
                        return@launch
                    }
                }

                val requestData = mapOf(
                    "requestId" to requestId,
                    "fromUid" to fromUid,
                    "fromName" to fromName,
                    "fromEmail" to fromEmail,
                    "fromFinCalcId" to fromFinCalcId,
                    "toUid" to toUid,
                    "toName" to (toUser.name.ifBlank { "User" }),
                    "toEmail" to (toUser.email),
                    "toFinCalcId" to (toUser.finCalcId),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )


                db.collection("friendRequests").document(requestId).set(requestData).await()
                _errorMessage.value = "Friend request sent!"
            } catch (e: Exception) {
                Log.e("FriendRequestError", "Failed to send request", e)
                _errorMessage.value = "Failed to send request. Please try again."
            }
        }
    }

    private suspend fun repairFriendship(uid1: String, uid2: String) {
        val sortedUids = listOf(uid1, uid2).sorted()
        val friendshipId = "${sortedUids[0]}_${sortedUids[1]}"
        val friendshipData = mapOf(
            "friendshipId" to friendshipId,
            "memberUids" to sortedUids,
            "user1Uid" to sortedUids[0],
            "user2Uid" to sortedUids[1],
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("friends").document(friendshipId).set(friendshipData).await()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchPendingRequests() {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Incoming requests
        pendingListener?.remove()
        pendingListener = db.collection("friendRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FriendRequestError", "Listen failed", e)
                    return@addSnapshotListener
                }
                try {
                    val requests = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(FriendRequest::class.java)
                    } ?: emptyList()
                    _pendingRequests.value = requests
                } catch (ex: Exception) {
                    Log.e("FriendRequestError", "Parsing failed", ex)
                }
            }

        // Outgoing requests
        sentListener?.remove()
        sentListener = db.collection("friendRequests")
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FriendRequestError", "Listen failed", e)
                    return@addSnapshotListener
                }
                try {
                    val requests = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(FriendRequest::class.java)
                    } ?: emptyList()
                    _sentRequests.value = requests
                } catch (ex: Exception) {
                    Log.e("FriendRequestError", "Parsing failed", ex)
                }
            }
    }

    fun acceptRequest(request: FriendRequest) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid != request.toUid) {
            _errorMessage.value = "You can only accept requests sent to you."
            return
        }

        viewModelScope.launch {
            try {
                // 1. Update request status
                db.collection("friendRequests").document(request.requestId)
                    .update(mapOf(
                        "status" to "accepted",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )).await()
                
                // 2. Create friendship record
                repairFriendship(request.fromUid, request.toUid)
                
                _errorMessage.value = "Friend request accepted!"
            } catch (e: Exception) {
                Log.e("FriendRequestError", "Accept failed", e)
                _errorMessage.value = "Accept failed. Please try again."
            }
        }
    }

    fun rejectRequest(request: FriendRequest) {
        viewModelScope.launch {
            try {
                db.collection("friendRequests").document(request.requestId)
                    .update(mapOf(
                        "status" to "rejected",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )).await()
                _errorMessage.value = "Request rejected."
            } catch (e: Exception) {
                Log.e("FriendRequestError", "Reject failed", e)
                _errorMessage.value = "Reject failed. Please try again."
            }
        }
    }

    fun fetchFriends() {
        val currentUid = auth.currentUser?.uid ?: return
        friendsListener?.remove()
        friendsListener = db.collection("friends")
            .whereArrayContains("memberUids", currentUid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FriendsError", "Listen failed", e)
                    return@addSnapshotListener
                }
                
                val friendUids = snapshot?.documents?.mapNotNull { doc ->
                    val members = doc["memberUids"] as? List<*>
                    members?.asSequence()?.filterIsInstance<String>()?.find { it != currentUid }
                } ?: emptyList()

                if (friendUids.isEmpty()) {
                    _friends.value = emptyList()
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    try {
                        val users = mutableListOf<User>()
                        for (uid in friendUids) {
                            val userDoc = db.collection("users").document(uid).get().await()
                            if (userDoc.exists()) {
                                userDoc.toObject(User::class.java)?.let { users.add(it) }
                            } else {
                                users.add(User(uid = uid, name = "Unknown User", finCalcId = uid))
                            }
                        }
                        _friends.value = users
                    } catch (ex: Exception) {
                        Log.e("FriendsError", "Failed to fetch friend profiles", ex)
                        _errorMessage.value = "Some friends could not be loaded."
                    }
                }
            }
    }
}
