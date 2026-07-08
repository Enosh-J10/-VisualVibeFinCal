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
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FriendsViewModel : ViewModel() {
    private val auth: com.google.firebase.auth.FirebaseAuth? by lazy { 
        try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (e: Throwable) { null } 
    }
    private val db: com.google.firebase.firestore.FirebaseFirestore? by lazy { 
        try { com.google.firebase.firestore.FirebaseFirestore.getInstance() } catch (e: Throwable) { null } 
    }

    init {
        logFirebaseDiagnostic("Init")
        fetchPendingRequests()
        fetchFriends()
        fetchFriendMetadata()
    }

        private fun logFirebaseDiagnostic(tag: String) {
    }

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests

    private val _sentRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val sentRequests: StateFlow<List<FriendRequest>> = _sentRequests

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends

    private val _friendNicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val friendNicknames: StateFlow<Map<String, String>> = _friendNicknames

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var pendingListener: ListenerRegistration? = null
    private var sentListener: ListenerRegistration? = null
    private var friendsListener: ListenerRegistration? = null
    private var nicknamesListener: ListenerRegistration? = null
    private var blockedListener: ListenerRegistration? = null

    override fun onCleared() {
        super.onCleared()
        pendingListener?.remove()
        sentListener?.remove()
        friendsListener?.remove()
        nicknamesListener?.remove()
        blockedListener?.remove()
    }

    fun searchUsers(query: String) {
        logFirebaseDiagnostic("searchUsers")
        if (query.trim().isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val currentUid = auth?.currentUser?.uid
                val users = UserUtils.searchUsers(query)
                val blocked = _blockedUsers.value
                _searchResults.value = users.filter { it.uid != currentUid && !blocked.contains(it.uid) }
            } catch (e: Exception) {
                Log.e("FriendsError", "Search failed", e)
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun sendFriendRequest(toUser: User) {
        logFirebaseDiagnostic("sendFriendRequest")
        val currentUserAuth = auth?.currentUser
        val authUid = currentUserAuth?.uid
        
        if (authUid == null || db == null) {
            _errorMessage.value = "Authentication service not available."
            return
        }

        val fromUid = authUid
        val toUid = toUser.uid

        if (toUid.isBlank()) {
            _errorMessage.value = "User not found."
            return
        }

        if (fromUid == toUid) {
            _errorMessage.value = "You cannot add yourself."
            return
        }

        viewModelScope.launch {
            try {
                val currentDb = db ?: throw Exception("Database not available")
                // Fetch current user profile from Firestore
                val currentUserDoc = currentDb.collection("users").document(fromUid).get().await()
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
                
                val friendDoc = currentDb.collection("friends").document(friendshipId).get().await()
                if (friendDoc.exists()) {
                    _errorMessage.value = "You are already friends."
                    return@launch
                }

                // Check if request already exists (either direction)
                val existingOut = currentDb.collection("friendRequests").document(requestId).get().await()
                val existingIn = currentDb.collection("friendRequests").document(reverseRequestId).get().await()
                
                if (existingOut.exists()) {
                    val status = existingOut.getString("status") ?: "pending"
                    if (status == "pending") {
                        _errorMessage.value = "Friend request already sent."
                        return@launch
                    } else if (status == "accepted") {
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
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                currentDb.collection("friendRequests").document(requestId).set(requestData).await()
                _errorMessage.value = "Friend request sent!"
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                _errorMessage.value = "Friend request failed: ${e.code} - ${e.message}"
            } catch (e: Exception) {
                Log.e("Friends", "Failed to send friend request", e)
                _errorMessage.value = "Unable to send request. Please try again."
            }
        }
    }

    private suspend fun repairFriendship(uid1: String, uid2: String) {
        val sortedUids = listOf(uid1, uid2).sorted()
        val friendshipId = "${sortedUids[0]}_${sortedUids[1]}"
        val friendship = mapOf(
            "friendshipId" to friendshipId,
            "memberUids" to sortedUids,
            "user1Uid" to sortedUids[0],
            "user2Uid" to sortedUids[1],
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db?.collection("friends")?.document(friendshipId)?.set(friendship, com.google.firebase.firestore.SetOptions.merge())?.await()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchPendingRequests() {
        logFirebaseDiagnostic("fetchPendingRequests")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        
        pendingListener?.remove()
        pendingListener = db?.collection("friendRequests")
            ?.whereEqualTo("toUid", currentUid)
            ?.whereEqualTo("status", "pending")
            ?.addSnapshotListener { snapshot, e ->
                if (e != null) {
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

        sentListener?.remove()
        sentListener = db?.collection("friendRequests")
            ?.whereEqualTo("fromUid", currentUid)
            ?.whereEqualTo("status", "pending")
            ?.addSnapshotListener { snapshot, e ->
                if (e != null) {
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
        logFirebaseDiagnostic("acceptRequest")
        val currentUid = auth?.currentUser?.uid ?: return
        if (currentUid != request.toUid || db == null) {
            _errorMessage.value = "Operation not available."
            return
        }

        viewModelScope.launch {
            try {
                val currentDb = db ?: throw Exception("Database not available")
                // Determine friendship ID (sorted UIDs)
                val uids = listOf(request.fromUid, request.toUid).sorted()
                val friendshipId = "${uids[0]}_${uids[1]}"

                // Data for the friends document
                val friendship = mapOf(
                    "friendshipId" to friendshipId,
                    "memberUids" to uids,
                    "user1Uid" to uids[0],
                    "user2Uid" to uids[1],
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                // Use a batch to update request and create friendship atomically
                val batch = currentDb.batch()
                
                val requestRef = currentDb.collection("friendRequests").document(request.requestId)
                batch.update(requestRef, mapOf(
                    "status" to "accepted",
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
                
                val friendshipRef = currentDb.collection("friends").document(friendshipId)
                batch.set(friendshipRef, friendship)
                
                batch.commit().await()
                
                _errorMessage.value = "Friend request accepted!"
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                _errorMessage.value = "Accept failed: ${e.code} - ${e.message}"
            } catch (e: Exception) {
                Log.e("Friends", "Accept failed", e)
                _errorMessage.value = "Accept failed. Please try again."
            }
        }
    }

    fun rejectRequest(request: FriendRequest) {
        logFirebaseDiagnostic("rejectRequest")
        if (db == null) return
        viewModelScope.launch {
            try {
                db?.collection("friendRequests")?.document(request.requestId)
                    ?.update(mapOf(
                        "status" to "rejected",
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ))?.await()
                _errorMessage.value = "Request rejected."
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                _errorMessage.value = "Decline failed: ${e.code} - ${e.message}"
            } catch (e: Exception) {
                _errorMessage.value = "Reject failed. Please try again."
            }
        }
    }

    fun cancelFriendRequest(request: FriendRequest) {
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        viewModelScope.launch {
            try {
                val requestId = if (request.requestId.isNotBlank()) request.requestId else "${request.fromUid}_${request.toUid}"
                val reverseRequestId = "${request.toUid}_${request.fromUid}"
                
                val doc = db?.collection("friendRequests")?.document(requestId)?.get()?.await()
                if (doc != null && doc.exists()) {
                    db?.collection("friendRequests")?.document(requestId)?.delete()?.await()
                    _errorMessage.value = "Friend request cancelled."
                } else {
                    val reverseDoc = db?.collection("friendRequests")?.document(reverseRequestId)?.get()?.await()
                    if (reverseDoc != null && reverseDoc.exists()) {
                        db?.collection("friendRequests")?.document(reverseRequestId)?.delete()?.await()
                        _errorMessage.value = "Friend request cancelled."
                    } else {
                        _errorMessage.value = "Friend request not found."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to cancel request"
            }
        }
    }

    fun fetchFriends() {
        logFirebaseDiagnostic("fetchFriends")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        friendsListener?.remove()
        friendsListener = db?.collection("friends")
            ?.whereArrayContains("memberUids", currentUid)
            ?.addSnapshotListener { snapshot, e ->
                if (e != null) {
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
                    val users = mutableListOf<User>()
                    for (uid in friendUids) {
                        if (_blockedUsers.value.contains(uid)) continue
                        
                        try {
                            val userDoc = db?.collection("users")?.document(uid)?.get()?.await()
                            if (userDoc != null && userDoc.exists()) {
                                userDoc.toObject(User::class.java)?.let { users.add(it) }
                            } else {
                                users.add(User(uid = uid, name = "Unknown User", finCalcId = "FIN ID unavailable"))
                            }
                        } catch (ex: Exception) {
                            Log.e("Friends", "Failed to fetch profile for $uid", ex)
                            users.add(User(uid = uid, name = "Unknown User", finCalcId = "FIN ID unavailable"))
                        }
                    }
                    _friends.value = users
                }
            }
    }

    private fun fetchFriendMetadata() {
        logFirebaseDiagnostic("fetchFriendMetadata")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        
        nicknamesListener?.remove()
        nicknamesListener = db?.collection("users")?.document(currentUid)
            ?.collection("friendSettings")
            ?.addSnapshotListener { snapshot, _ ->
                val nicknames = snapshot?.documents?.associate { doc ->
                    doc.id to (doc.getString("nickname") ?: "")
                } ?: emptyMap()
                _friendNicknames.value = nicknames
            }

        blockedListener?.remove()
        blockedListener = db?.collection("users")?.document(currentUid)
            ?.collection("blockedUsers")
            ?.addSnapshotListener { snapshot, _ ->
                val blocked = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                _blockedUsers.value = blocked
                fetchFriends()
            }
    }

    fun setNickname(friendUid: String, nickname: String) {
        logFirebaseDiagnostic("setNickname")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        viewModelScope.launch {
            try {
                db?.collection("users")?.document(currentUid)
                    ?.collection("friendSettings")?.document(friendUid)
                    ?.set(mapOf(
                        "nickname" to nickname,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ))?.await()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set nickname."
            }
        }
    }

    fun removeFriend(friendUid: String) {
        logFirebaseDiagnostic("removeFriend")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        viewModelScope.launch {
            try {
                val uids = listOf(currentUid, friendUid).sorted()
                val friendshipId = "${uids[0]}_${uids[1]}"
                
                val currentDb = db ?: throw Exception("Database not available")
                val batch = currentDb.batch()
                
                val friendshipRef = currentDb.collection("friends").document(friendshipId)
                batch.delete(friendshipRef)
                
                val req1Ref = currentDb.collection("friendRequests").document("${currentUid}_$friendUid")
                batch.delete(req1Ref)
                
                val req2Ref = currentDb.collection("friendRequests").document("${friendUid}_$currentUid")
                batch.delete(req2Ref)
                
                batch.commit().await()
                
                _errorMessage.value = "Friend removed."
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                _errorMessage.value = "Remove failed: ${e.code} - ${e.message}"
            } catch (e: Exception) {
                Log.e("Friends", "Remove failed", e)
                _errorMessage.value = "Failed to remove friend."
            }
        }
    }

    fun blockUser(blockedUid: String) {
        logFirebaseDiagnostic("blockUser")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        viewModelScope.launch {
            try {
                removeFriend(blockedUid)
                
                db?.collection("users")?.document(currentUid)
                    ?.collection("blockedUsers")?.document(blockedUid)
                    ?.set(mapOf(
                        "blockedUid" to blockedUid,
                        "blockedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ))?.await()
                
                _errorMessage.value = "User blocked."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to block user."
            }
        }
    }

    fun unblockUser(blockedUid: String) {
        logFirebaseDiagnostic("unblockUser")
        val currentUid = auth?.currentUser?.uid ?: return
        if (db == null) return
        viewModelScope.launch {
            try {
                db?.collection("users")?.document(currentUid)
                    ?.collection("blockedUsers")?.document(blockedUid)
                    ?.delete()?.await()
                _errorMessage.value = "User unblocked."
            } catch (e: Exception) {
                _errorMessage.value = "Failed to unblock user."
            }
        }
    }
}
