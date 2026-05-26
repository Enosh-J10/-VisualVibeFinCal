package com.enosh.fincalc.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.*
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.ui.screens.BouncyButton
import com.enosh.fincalc.ui.screens.CalculatorCard
import com.enosh.fincalc.ui.screens.CalculatorScreenScaffold
import com.enosh.fincalc.ui.screens.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBookScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("NotesPrefs", Context.MODE_PRIVATE) }
    
    var notes by remember { 
        mutableStateOf(
            sharedPref.all.map { (key, value) -> 
                val parts = value.toString().split("::::", limit = 2)
                val (title, content) = if (parts.size > 1) parts[0] to parts[1] else "No Title" to parts[0]
                Note(key, title, content, key.toLongOrNull() ?: 0L) 
            }.sortedByDescending { it.timestamp }
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var noteTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var noteTitleError by remember { mutableStateOf<String?>(null) }
    var noteTextError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingNote) {
        if (editingNote != null) {
            noteTitle = editingNote!!.title
            noteText = editingNote!!.content
            showAddDialog = true
        }
    }

    CalculatorScreenScaffold(
        title = "Note Book",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No notes yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        NoteItem(
                            note = note, 
                            isDarkMode = isDarkMode, 
                            onDelete = {
                                assistantViewModel.showMessage("Note deleted", AssistantState.ERROR)
                                sharedPref.edit { remove(note.id) }
                                notes = notes.filter { it.id != note.id }
                            },
                            onEdit = {
                                editingNote = note
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = Color(0xFF00D1B2),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editingNote = null
                noteTitle = ""
                noteText = ""
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { Text(if (editingNote == null) "Add Note" else "Edit Note", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ValidatedTextField(
                        value = noteTitle,
                        onValueChange = { 
                            noteTitle = it
                            noteTitleError = if (it.isBlank()) "Title is required" else null
                        },
                        label = "Title",
                        error = noteTitleError,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                    )
                    Spacer(Modifier.height(12.dp))
                    ValidatedTextField(
                        value = noteText,
                        onValueChange = { 
                            noteText = it
                            noteTextError = if (it.isBlank()) "Content is required" else null
                        },
                        label = "Content",
                        error = noteTextError,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        modifier = Modifier.height(150.dp)
                    )
                }
            },
            confirmButton = {
                BouncyButton(
                    onClick = {
                        if (noteText.isNotBlank() && noteTitle.isNotBlank()) {
                            val id = editingNote?.id ?: System.currentTimeMillis().toString()
                            val combinedData = "$noteTitle::::$noteText"
                            sharedPref.edit { putString(id, combinedData) }
                            
                            val newNote = Note(id, noteTitle, noteText, id.toLong())
                            if (editingNote == null) {
                                notes = (listOf(newNote) + notes).sortedByDescending { it.timestamp }
                                assistantViewModel.showMessage("Note saved!", AssistantState.HAPPY)
                            } else {
                                notes = notes.map { if (it.id == id) newNote else it }
                                assistantViewModel.showMessage("Note updated!", AssistantState.HAPPY)
                            }
                            
                            noteText = ""
                            noteTitle = ""
                            editingNote = null
                            showAddDialog = false
                        }
                    },
                    enabled = noteTitle.isNotBlank() && noteText.isNotBlank() && noteTitleError == null && noteTextError == null,
                    modifier = Modifier.fillMaxWidth(0.4f)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    editingNote = null
                    noteTitle = ""
                    noteText = ""
                }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}

data class Note(val id: String, val title: String, val content: String, val timestamp: Long)

@Composable
fun NoteItem(note: Note, isDarkMode: Boolean, onDelete: () -> Unit, onEdit: () -> Unit) {
    val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = (if (isDarkMode) Color(0xFF1B2C33) else Color.White).copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(date, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            if (note.title.isNotBlank()) {
                Text(
                    note.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(
                note.content,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.8f)
            )
        }
    }
}
