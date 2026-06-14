package com.enosh.fincalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    private val _searchResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val searchResults: StateFlow<List<Map<String, Any>>> = _searchResults

    fun fetchTrips() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("trips")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                _trips.value = snapshot?.toObjects(TravelTrip::class.java) ?: emptyList()
            }
    }

    fun createTrip(name: String, destination: String, startDate: Long, endDate: Long, currency: String, description: String) {
        val uid = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: ""
        val userName = auth.currentUser?.displayName ?: "Unknown"
        
        val tripId = db.collection("trips").document().id
        val trip = TravelTrip(
            id = tripId,
            name = name,
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            currency = currency,
            description = description,
            adminId = uid,
            members = listOf(uid),
            memberDetails = mapOf(uid to MemberInfo(name = userName, email = userEmail, status = "JOINED"))
        )
        db.collection("trips").document(tripId).set(trip)
    }

    fun addMember(tripId: String, memberUid: String, name: String, email: String) {
        db.collection("trips").document(tripId).get().addOnSuccessListener { snapshot ->
            val trip = snapshot.toObject(TravelTrip::class.java) ?: return@addOnSuccessListener
            val newMembers = trip.members.toMutableList()
            if (!newMembers.contains(memberUid)) {
                newMembers.add(memberUid)
                val newDetails = trip.memberDetails.toMutableMap()
                newDetails[memberUid] = MemberInfo(name = name, email = email, status = "INVITED")
                db.collection("trips").document(tripId).update(
                    "members", newMembers,
                    "memberDetails", newDetails
                )
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }
        db.collection("users")
            .whereGreaterThanOrEqualTo("searchName", query.lowercase())
            .whereLessThanOrEqualTo("searchName", query.lowercase() + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                _searchResults.value = snapshot.documents.mapNotNull { it.data }
            }
    }

    fun fetchExpenses(tripId: String) {
        db.collection("trips").document(tripId).collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                _currentTripExpenses.value = snapshot?.toObjects(TravelExpense::class.java) ?: emptyList()
            }
    }

    fun addExpense(tripId: String, expense: TravelExpense) {
        val docId = db.collection("trips").document(tripId).collection("expenses").document().id
        db.collection("trips").document(tripId).collection("expenses").document(docId).set(expense.copy(id = docId))
    }

    fun finalizeTrip(tripId: String) {
        db.collection("trips").document(tripId).update("isFinalized", true)
    }

    fun calculateSettlements(trip: TravelTrip, expenses: List<TravelExpense>): List<String> {
        val members = trip.members
        val details = trip.memberDetails
        val totalCost = expenses.sumOf { it.amount }
        if (members.isEmpty()) return emptyList()
        val share = totalCost / members.size
        
        val balances = members.map { uid ->
            val paid = expenses.filter { it.paidBy == uid }.sumOf { it.amount }
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
                results.add("$debtorName owes $creditorName: ${String.format("%.2f", amount)}")
            }
            
            balances[i] = creditor.first to creditor.second - amount
            balances[j] = debtor.first to debtor.second + amount
            
            if (balances[i].second < 0.01) i++
            if (balances[j].second > -0.01) j--
        }
        
        return if (results.isEmpty()) listOf("All settled!") else results
    }
}
