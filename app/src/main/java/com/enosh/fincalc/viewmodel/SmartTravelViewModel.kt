package com.enosh.fincalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.enosh.fincalc.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.enosh.fincalc.utils.UserUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SmartTravelViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _trips = MutableStateFlow<List<TravelTrip>>(emptyList())
    val trips: StateFlow<List<TravelTrip>> = _trips

    private val _currentTripExpenses = MutableStateFlow<List<TravelExpense>>(emptyList())
    val currentTripExpenses: StateFlow<List<TravelExpense>> = _currentTripExpenses

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _expenseFlags = MutableStateFlow<Map<String, List<TravelExpenseFlag>>>(emptyMap())
    val expenseFlags: StateFlow<Map<String, List<TravelExpenseFlag>>> = _expenseFlags

    private val _memberProfiles = MutableStateFlow<Map<String, User>>(emptyMap())
    val memberProfiles: StateFlow<Map<String, User>> = _memberProfiles

    private var tripsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var invitedTripsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var expensesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val flagListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    override fun onCleared() {
        super.onCleared()
        tripsListener?.remove()
        invitedTripsListener?.remove()
        expensesListener?.remove()
        flagListeners.values.forEach { it.remove() }
        flagListeners.clear()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchTrips() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        
        tripsListener?.remove()
        tripsListener = db.collection("trips")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SmartTravelError", "Listen failed: collection=trips, uid=$uid", e)
                    return@addSnapshotListener
                }
                
                val trips = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val trip = doc.toObject(TravelTrip::class.java) ?: return@mapNotNull null
                        
                        // Compatibility fallbacks
                        val legacyAdmin = doc.getString("adminId") ?: ""
                        val legacyCurrency = doc.getString("currency") ?: ""
                        val legacyMembersRaw = doc["members"] as? List<*> ?: emptyList<Any?>()
                        val membersList = legacyMembersRaw.filterIsInstance<String>()
                        
                        trip.copy(
                            tripId = doc.id,
                            createdByUid = trip.createdByUid.ifEmpty { legacyAdmin },
                            memberUids = trip.memberUids.ifEmpty { membersList },
                            currencyCode = trip.currencyCode.ifEmpty { legacyCurrency }
                        )
                    } catch (ex: Exception) {
                        Log.e("SmartTravel", "Error parsing trip ${doc.id}", ex)
                        null
                    }
                } ?: emptyList()
                
                _trips.value = trips
            }
            
        // Also fetch trips where user is invited
        invitedTripsListener?.remove()
        invitedTripsListener = db.collection("trips")
            .whereArrayContains("invitedUids", uid)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    val invitedTrips = snapshot.documents.mapNotNull { it.toObject(TravelTrip::class.java) }
                    val currentList = _trips.value.toMutableList()
                    invitedTrips.forEach { itrip ->
                        if (currentList.none { it.tripId == itrip.tripId }) {
                            currentList.add(itrip)
                        }
                    }
                    _trips.value = currentList
                }
            }
    }

    fun createTrip(
        tripName: String,
        selectedCountryName: String,
        destinationCity: String,
        selectedCurrencyCode: String,
        selectedCurrencySymbol: String,
        description: String = ""
    ) {
        val currentUser = auth.currentUser ?: run {
            _errorMessage.value = "Please log in to create shared trips."
            return
        }
        val uid = currentUser.uid
        
        viewModelScope.launch {
            try {
                val tripRef = db.collection("trips").document()
                val tripId = tripRef.id

                val tripData = mapOf(
                    "tripId" to tripId,
                    "name" to tripName.take(100).trim(),
                    "destinationCountry" to selectedCountryName.take(100),
                    "destinationCity" to destinationCity.take(100).trim(),
                    "currencyCode" to selectedCurrencyCode.take(10),
                    "currencySymbol" to selectedCurrencySymbol.take(10),
                    "createdByUid" to uid,
                    "memberUids" to listOf(uid),
                    "invitedUids" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "description" to description.take(500).trim(),
                    "memberDetails" to mapOf(
                        uid to MemberInfo(
                            name = (currentUser.displayName ?: "User").take(100),
                            email = (currentUser.email ?: "").take(100),
                            status = "JOINED"
                        )
                    )
                )

                tripRef.set(tripData).await()
                _errorMessage.value = "Trip Created"
            } catch (e: Exception) {
                Log.e("SmartTravelError", "Failed to create trip: collection=trips, uid=$uid", e)
                _errorMessage.value = "Failed to create trip: ${e.message}"
            }
        }
    }

    fun updateTrip(
        tripId: String,
        name: String,
        destinationCountry: String,
        destinationCity: String,
        currencyCode: String,
        currencySymbol: String,
        description: String
    ) {
        viewModelScope.launch {
            try {
                db.collection("trips").document(tripId).update(
                    "name", name.trim(),
                    "destinationCountry", destinationCountry,
                    "destinationCity", destinationCity.trim(),
                    "currencyCode", currencyCode,
                    "currencySymbol", currencySymbol,
                    "description", description.trim(),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
                _errorMessage.value = "Trip Updated"
            } catch (e: Exception) {
                Log.e("SmartTravel", "Update trip failed", e)
                _errorMessage.value = "Failed to update trip"
            }
        }
    }

    fun deleteTrip(tripId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val tripDocRef = db.collection("trips").document(tripId)
                
                // 1. Delete all expenses and their flags
                val expensesSnapshot = tripDocRef.collection("expenses").get().await()
                
                for (expenseDoc in expensesSnapshot.documents) {
                    val flagsSnapshot = expenseDoc.reference.collection("flags").get().await()
                    
                    val subBatch = db.batch()
                    for (flagDoc in flagsSnapshot.documents) {
                        subBatch.delete(flagDoc.reference)
                    }
                    subBatch.delete(expenseDoc.reference)
                    subBatch.commit().await()
                }
                
                // 2. Delete other possible subcollections. Best effort as they might not always be used
                val subcollections = listOf("members", "settlements", "receipts")
                for (sub in subcollections) {
                    val snap = tripDocRef.collection(sub).get().await()
                    if (!snap.isEmpty) {
                        val batch = db.batch()
                        snap.documents.forEach { batch.delete(it.reference) }
                        batch.commit().await()
                    }
                }
                
                // 3. Delete the trip document itself
                tripDocRef.delete().await()
                
                // Update local state
                val currentTrips = _trips.value.toMutableList()
                currentTrips.removeAll { it.tripId == tripId }
                _trips.value = currentTrips

                _errorMessage.value = "Trip deleted successfully"
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete trip: ${e.message}"
            }
        }
    }

    fun addMember(tripId: String, memberUid: String, name: String, email: String) {
        db.collection("trips").document(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.toObject(TravelTrip::class.java) ?: return@addOnSuccessListener
            
            val invited = trip.invitedUids.toMutableList()
            if (!invited.contains(memberUid) && !trip.memberUids.contains(memberUid)) {
                invited.add(memberUid)
                val newDetails = trip.memberDetails.toMutableMap()
                newDetails[memberUid] = MemberInfo(name = name, email = email, status = "INVITED")
                db.collection("trips").document(tripId).update(
                    "invitedUids", invited,
                    "memberDetails", newDetails
                ).addOnFailureListener { e ->
                    Log.e("SmartTravelError", "Add member failed", e)
                }
            }
        }
    }

    fun joinTrip(tripId: String) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        
        db.collection("trips").document(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.toObject(TravelTrip::class.java) ?: return@addOnSuccessListener
            if (trip.invitedUids.contains(uid)) {
                val invited = trip.invitedUids.toMutableList()
                invited.remove(uid)
                
                val members = trip.memberUids.toMutableList()
                if (!members.contains(uid)) members.add(uid)
                
                val details = trip.memberDetails.toMutableMap()
                details[uid] = MemberInfo(
                    name = currentUser.displayName ?: "User",
                    email = currentUser.email ?: "",
                    status = "JOINED"
                )
                
                db.collection("trips").document(tripId).update(
                    "invitedUids", invited,
                    "memberUids", members,
                    "memberDetails", details,
                    "updatedAt", FieldValue.serverTimestamp()
                )
            }
        }
    }

    fun removeMember(tripId: String, memberUid: String) {
        db.collection("trips").document(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.toObject(TravelTrip::class.java) ?: return@addOnSuccessListener
            
            val invited = trip.invitedUids.toMutableList()
            invited.remove(memberUid)
            
            val members = trip.memberUids.toMutableList()
            members.remove(memberUid)
            
            val details = trip.memberDetails.toMutableMap()
            details.remove(memberUid)
            
            db.collection("trips").document(tripId).update(
                "invitedUids", invited,
                "memberUids", members,
                "memberDetails", details,
                "updatedAt", FieldValue.serverTimestamp()
            )
        }
    }

    fun searchUsers(query: String) {
        if (query.trim().isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _searchResults.value = UserUtils.searchUsers(query)
            } catch (e: Exception) {
                Log.e("SmartTravel", "Search failed", e)
            }
        }
    }

    fun fetchMemberProfiles(uids: List<String>) {
        if (uids.isEmpty()) return
        viewModelScope.launch {
            try {
                val profiles = mutableMapOf<String, User>()
                for (uid in uids) {
                    val doc = db.collection("users").document(uid).get().await()
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        profiles[uid] = user
                    }
                }
                _memberProfiles.value = _memberProfiles.value + profiles
            } catch (e: Exception) {
                Log.e("SmartTravel", "Failed to fetch member profiles", e)
            }
        }
    }

    fun fetchExpenses(tripId: String) {
        if (tripId.isBlank()) return
        
        expensesListener?.remove()
        expensesListener = db.collection("trips").document(tripId).collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SmartTravel", "Expense listener failed", e)
                    return@addSnapshotListener
                }
                
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val expense = doc.toObject(TravelExpense::class.java) ?: return@mapNotNull null
                        fetchFlags(tripId, doc.id)
                        expense.copy(expenseId = doc.id)
                    } catch (ex: Exception) {
                        null
                    }
                } ?: emptyList()
                
                _currentTripExpenses.value = expenses
                
                // Fetch profiles for all creators/payers to ensure actual names are shown
                val uidsToFetch = expenses.flatMap { listOf(it.createdByUid, it.paidByUid) }.distinct()
                if (uidsToFetch.isNotEmpty()) {
                    fetchMemberProfiles(uidsToFetch)
                }
            }
    }

    private fun fetchFlags(tripId: String, expenseId: String) {
        flagListeners[expenseId]?.remove()
        flagListeners[expenseId] = db.collection("trips").document(tripId).collection("expenses").document(expenseId)
            .collection("flags").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SmartTravel", "fetchFlags failed for $expenseId", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val flags = snapshot.documents.mapNotNull { 
                        try {
                            it.toObject(TravelExpenseFlag::class.java)?.copy(flagId = it.id)
                        } catch(ex: Exception) {
                            Log.e("SmartTravel", "Error parsing flag ${it.id}", ex)
                            null
                        }
                    }
                    val currentFlags = _expenseFlags.value.toMutableMap()
                    currentFlags[expenseId] = flags
                    _expenseFlags.value = currentFlags
                }
            }
    }

    fun addExpense(tripId: String, expense: TravelExpense) {
        val uid = auth.currentUser?.uid ?: ""
        val expenseRef = db.collection("trips").document(tripId).collection("expenses").document()
        val expenseId = expenseRef.id
        
        val expenseToSave = expense.copy(
            expenseId = expenseId,
            tripId = tripId,
            createdByUid = uid,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        expenseRef.set(expenseToSave)
    }

    fun updateExpense(tripId: String, expense: TravelExpense) {
        db.collection("trips").document(tripId).collection("expenses").document(expense.expenseId).set(expense.copy(updatedAt = System.currentTimeMillis()))
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        viewModelScope.launch {
            try {
                val expenseRef = db.collection("trips").document(tripId).collection("expenses").document(expenseId)
                
                // 1. Delete nested flags
                val flagsSnapshot = expenseRef.collection("flags").get().await()
                
                val batch = db.batch()
                flagsSnapshot.documents.forEach { batch.delete(it.reference) }
                
                // 2. Delete expense
                batch.delete(expenseRef)
                batch.commit().await()
                
                // 3. Remove from local state immediately
                val currentExpenses = _currentTripExpenses.value.toMutableList()
                currentExpenses.removeAll { it.expenseId == expenseId }
                _currentTripExpenses.value = currentExpenses
                
                _errorMessage.value = "Expense deleted"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete expense: ${e.message}"
            }
        }
    }

    fun flagExpense(tripId: String, expenseId: String, reason: String, note: String) {
        val user = auth.currentUser ?: return
        val flagId = java.util.UUID.randomUUID().toString()
        val flag = TravelExpenseFlag(
            flagId = flagId,
            expenseId = expenseId,
            tripId = tripId,
            createdByUid = user.uid,
            createdByName = user.displayName ?: "User",
            reasonType = reason,
            note = note,
            createdAt = com.google.firebase.Timestamp.now(),
            status = "open"
        )
        
        db.collection("trips").document(tripId).collection("expenses").document(expenseId)
            .collection("flags").document(flagId).set(flag).addOnSuccessListener {
                db.collection("trips").document(tripId).collection("expenses").document(expenseId)
                    .update("hasOpenFlags", true)
            }.addOnFailureListener {
                Log.e("SmartTravel", "Failed to create flag", it)
            }
    }

    fun resolveFlag(tripId: String, expenseId: String, flagId: String) {
        // The requirement says "Resolved flags should disappear after confirmation."
        // We delete the flag document to make it disappear.
        db.collection("trips").document(tripId).collection("expenses").document(expenseId)
            .collection("flags").document(flagId).delete().addOnSuccessListener {
                // Check if any open flags remain
                db.collection("trips").document(tripId).collection("expenses").document(expenseId)
                    .collection("flags").get().addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            db.collection("trips").document(tripId).collection("expenses").document(expenseId)
                                .update("hasOpenFlags", false)
                        }
                    }
            }.addOnFailureListener {
                Log.e("SmartTravel", "Failed to resolve/delete flag", it)
            }
    }

    fun finalizeTrip(tripId: String) {
        db.collection("trips").document(tripId).update(
            "isFinalized", true,
            "finalizedAt", FieldValue.serverTimestamp(),
            "finalizedByUid", auth.currentUser?.uid
        )
    }

    fun reopenTrip(tripId: String) {
        db.collection("trips").document(tripId).update("isFinalized", false)
    }

    fun calculateSettlements(trip: TravelTrip, expenses: List<TravelExpense>, profiles: Map<String, User> = emptyMap()): List<String> {
        val members = trip.memberUids
        if (members.isEmpty()) return emptyList()

        val netBalances = mutableMapOf<String, Double>()
        members.forEach { netBalances[it] = 0.0 }

        expenses.forEach { expense ->
            val paidBy = expense.paidByUid
            val amount = expense.amount
            
            val splitAmong = when (expense.splitType) {
                "EQUAL" -> members
                "EXCLUDE" -> members.filter { it !in expense.excludedMembers }
                "CUSTOM" -> expense.customSplits.keys.toList()
                else -> members
            }
            
            if (splitAmong.isNotEmpty()) {
                val perPerson = if (expense.splitType == "CUSTOM") 0.0 else amount / splitAmong.size
                
                netBalances[paidBy] = (netBalances[paidBy] ?: 0.0) + amount
                
                splitAmong.forEach { uid ->
                    val share = if (expense.splitType == "CUSTOM") expense.customSplits[uid] ?: 0.0 else perPerson
                    netBalances[uid] = (netBalances[uid] ?: 0.0) - share
                }
            }
        }

        val creditors = netBalances.filter { it.value > 0.01 }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val debtors = netBalances.filter { it.value < -0.01 }
            .map { it.key to -it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val settlements = mutableListOf<String>()
        var cIdx = 0
        var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val creditor = creditors[cIdx]
            val debtor = debtors[dIdx]
            
            val amount = kotlin.math.min(creditor.second, debtor.second)
            if (amount > 0.01) {
                val cNameRaw = profiles[creditor.first]?.name ?: trip.memberDetails[creditor.first]?.name ?: "Someone"
                val dNameRaw = profiles[debtor.first]?.name ?: trip.memberDetails[debtor.first]?.name ?: "Someone"
                
                val cName = if (cNameRaw.equals("Me", true)) (auth.currentUser?.displayName ?: "You") else cNameRaw
                val dName = if (dNameRaw.equals("Me", true)) (auth.currentUser?.displayName ?: "You") else dNameRaw

                settlements.add("$dName owes $cName ${trip.currencySymbol}${String.format(java.util.Locale.getDefault(), "%.2f", amount)}")
            }

            creditors[cIdx] = creditor.first to (creditor.second - amount)
            debtors[dIdx] = debtor.first to (debtor.second - amount)

            if (creditors[cIdx].second < 0.01) cIdx++
            if (debtors[dIdx].second < 0.01) dIdx++
        }

        return if (settlements.isEmpty()) listOf("All settled up!") else settlements
    }
}
