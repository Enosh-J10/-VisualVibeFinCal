package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.enosh.fincalc.data.local.entity.Goal
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.FinancialViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    financialViewModel: FinancialViewModel = viewModel()
) {
    val goals by financialViewModel.allGoals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    CalculatorScreenScaffold(
        title = "Savings Goals",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (goals.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No goals yet. Start saving today! 🎯", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(goals) { goal ->
                        GoalItem(goal, isDarkMode, 
                            onDelete = { financialViewModel.deleteGoal(goal) },
                            onEdit = { editingGoal = goal }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = Color(0xFF00D1B2)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = Color.White)
            }
        }
    }

    if (showAddDialog || editingGoal != null) {
        GoalDialog(
            goal = editingGoal,
            onDismiss = { 
                showAddDialog = false
                editingGoal = null
            },
            onSave = { name, target, saved ->
                if (editingGoal == null) {
                    financialViewModel.insertGoal(Goal(name = name, targetAmount = target, savedAmount = saved, deadline = System.currentTimeMillis()))
                } else {
                    financialViewModel.updateGoal(editingGoal!!.copy(name = name, targetAmount = target, savedAmount = saved))
                }
                showAddDialog = false
                editingGoal = null
            }
        )
    }
}

@Composable
fun GoalItem(goal: Goal, isDarkMode: Boolean, onDelete: () -> Unit, onEdit: () -> Unit) {
    val progress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text("$${String.format("%.2f", goal.savedAmount)} of $${String.format("%.2f", goal.targetAmount)}", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF00D1B2),
                trackColor = Color.Gray.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(Modifier.height(4.dp))
            Text("${(progress * 100).toInt()}% complete", fontSize = 12.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun GoalDialog(goal: Goal?, onDismiss: () -> Unit, onSave: (String, Double, Double) -> Unit) {
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var targetStr by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }
    var savedStr by remember { mutableStateOf(goal?.savedAmount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "New Savings Goal" else "Edit Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ValidatedTextField(value = name, onValueChange = { name = it }, label = "Goal Name")
                ValidatedTextField(value = targetStr, onValueChange = { targetStr = ValidationUtils.formatNumericInput(it) }, label = "Target Amount")
                ValidatedTextField(value = savedStr, onValueChange = { savedStr = ValidationUtils.formatNumericInput(it) }, label = "Currently Saved")
            }
        },
        confirmButton = {
            Button(onClick = {
                val target = targetStr.toDoubleOrNull() ?: 0.0
                val saved = savedStr.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && target > 0) {
                    onSave(name, target, saved)
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
