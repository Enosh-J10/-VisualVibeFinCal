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

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchTrips() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("trips")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SmartTravelError", "Listen failed", e)
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
    }

    fun createTrip(
        tripName: String,
        selectedCountryName: String,
        destinationCity: String,
        selectedCurrencyCode: String,
        selectedCurrencySymbol: String
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
                    "name" to tripName.trim(),
                    "destinationCountry" to selectedCountryName,
                    "destinationCity" to destinationCity.trim(),
                    "currencyCode" to selectedCurrencyCode,
                    "currencySymbol" to selectedCurrencySymbol,
                    "createdByUid" to uid,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                tripRef.set(tripData).await()
            } catch (e: Exception) {
                Log.e("SmartTravelError", "Failed to create trip", e)
                _errorMessage.value = "Failed to create trip: ${e.message}"
            }
        }
    }

    fun addMember(tripId: String, memberUid: String, name: String, email: String) {
        db.collection("trips").document(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.toObject(TravelTrip::class.java) ?: return@addOnSuccessListener
            
            // Compatibility for old trips using 'members' field
            val legacyMembers = (snapshot["members"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val currentMembers = trip.memberUids.ifEmpty { legacyMembers }

            if (!currentMembers.contains(memberUid)) {
                val newMembers = currentMembers.toMutableList()
                newMembers.add(memberUid)
                val newDetails = trip.memberDetails.toMutableMap()
                newDetails[memberUid] = MemberInfo(name = name, email = email, status = "INVITED")
                db.collection("trips").document(tripId).update(
                    "memberUids", newMembers,
                    "memberDetails", newDetails
                )
            }
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

    fun fetchExpenses(tripId: String) {
        if (tripId.isBlank()) {
            Log.e("SmartTravel", "fetchExpenses: tripId is blank")
            return
        }
        
        db.collection("trips").document(tripId).collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SmartTravel", "Expense listener failed for trip $tripId", e)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    _currentTripExpenses.value = emptyList()
                    return@addSnapshotListener
                }

                val expenses = snapshot.documents.mapNotNull { doc ->
                    try {
                        val expense = doc.toObject(TravelExpense::class.java) ?: return@mapNotNull null
                        
                        // Compatibility fallbacks
                        val legacyPaidBy = doc.getString("paidBy") ?: ""
                        val legacyDate = doc.getLong("date") ?: 0L

                        expense.copy(
                            expenseId = doc.id,
                            paidByUid = if (expense.paidByUid.isEmpty()) legacyPaidBy else expense.paidByUid,
                            createdAt = if (expense.createdAt == 0L) legacyDate else expense.createdAt
                        )
                    } catch (ex: Exception) {
                        Log.e("SmartTravel", "Error parsing expense ${doc.id}", ex)
                        null
                    }
                }
                
                _currentTripExpenses.value = expenses
            }
    }

    fun addExpense(tripId: String, expense: TravelExpense) {
        if (tripId.isBlank()) {
            Log.e("SmartTravel", "addExpense: tripId is blank")
            return
        }
        
        val uid = auth.currentUser?.uid ?: ""
        val expenseRef = db.collection("trips").document(tripId).collection("expenses").document()
        val expenseId = expenseRef.id
        
        val expenseToSave = expense.copy(
            expenseId = expenseId,
            tripId = tripId,
            createdByUid = uid,
            createdAt = if (expense.createdAt != 0L) expense.createdAt else System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        expenseRef.set(expenseToSave)
            .addOnFailureListener { e ->
                Log.e("SmartTravel", "Failed to save expense: $expenseId", e)
                _errorMessage.value = "Failed to add expense: ${e.message}"
            }
    }

    fun updateExpense(tripId: String, expense: TravelExpense) {
        val expenseId = expense.expenseId
        if (expenseId.isEmpty() || tripId.isEmpty()) {
            Log.e("SmartTravel", "updateExpense: expenseId or tripId is empty")
            return
        }
        
        val expenseToSave = expense.copy(
            updatedAt = System.currentTimeMillis()
        )
        
        db.collection("trips").document(tripId).collection("expenses").document(expenseId).set(expenseToSave)
            .addOnFailureListener { e ->
                Log.e("SmartTravel", "Expense update failed: $expenseId", e)
                _errorMessage.value = "Failed to update expense: ${e.message}"
            }
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        if (tripId.isEmpty() || expenseId.isEmpty()) {
            Log.e("SmartTravel", "deleteExpense: tripId or expenseId is empty")
            return
        }
        db.collection("trips").document(tripId).collection("expenses").document(expenseId).delete()
            .addOnFailureListener { e ->
                Log.e("SmartTravel", "Expense deletion failed: $expenseId", e)
            }
    }

    fun finalizeTrip(tripId: String) {
        db.collection("trips").document(tripId).update("isFinalized", true)
    }

    fun calculateSettlements(trip: TravelTrip, expenses: List<TravelExpense>): List<String> {
        val members = trip.memberUids
        val details = trip.memberDetails
        val totalCost = expenses.sumOf { it.amount }
        if (members.isEmpty()) return emptyList()
        val share = totalCost / members.size
        
        val balances = members.map { uid ->
            val paid = expenses.filter { it.paidByUid == uid }.sumOf { it.amount }
            uid to paid - share
        }.filter { kotlin.math.abs(it.second) > 0.01 }.toMutableList()

        val results = mutableListOf<String>()
        balances.sortByDescending { it.second }
        
        var i = 0
        var j = balances.size - 1
        
        while (i < j) {
            val creditor = balances[i]
            val debtor = balances[j]
            
            val amount = kotlin.math.min(creditor.second, -debtor.second)
            if (amount > 0.01) {
                val creditorName = details[creditor.first]?.name ?: "Unknown"
                val debtorName = details[debtor.first]?.name ?: "Unknown"
                results.add("$debtorName owes $creditorName: ${String.format(java.util.Locale.getDefault(), "%.2f", amount)}")
            }
            
            balances[i] = creditor.first to creditor.second - amount
            balances[j] = debtor.first to debtor.second + amount
            
            if (balances[i].second < 0.01) i++
            if (balances[j].second > -0.01) j--
        }
        
        return if (results.isEmpty()) listOf("All settled!") else results
    }
}
