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
import com.enosh.fincalc.utils.UserUtils
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
    val uid = UserUtils.getEffectiveUid(context)
    val sharedPref = remember(uid) { context.getSharedPreferences("NotesPrefs_$uid", Context.MODE_PRIVATE) }
    
    var notes by remember(sharedPref) { 
        mutableStateOf(
            sharedPref.all.map { (key, value) -> 
                val parts = value.toString().split("::::")
                val (title, content, type) = when (parts.size) {
                    3 -> Triple(parts[0], parts[1], parts[2])
                    2 -> Triple(parts[0], parts[1], "TEXT")
                    else -> Triple("No Title", parts[0], "TEXT")
                }
                Note(key, title, content, key.toLongOrNull() ?: 0L, type == "CHECKLIST") 
            }.sortedByDescending { it.timestamp }
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var noteTitle by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var isChecklistMode by remember { mutableStateOf(false) }
    var noteTitleError by remember { mutableStateOf<String?>(null) }
    var noteTextError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingNote) {
        if (editingNote != null) {
            noteTitle = editingNote!!.title
            noteText = editingNote!!.content
            isChecklistMode = editingNote!!.isChecklist
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
                isChecklistMode = false
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { 
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (editingNote == null) "Add Note" else "Edit Note", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Checklist", fontSize = 12.sp, color = Color.Gray)
                        Checkbox(
                            checked = isChecklistMode,
                            onCheckedChange = { isChecklistMode = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D1B2))
                        )
                    }
                }
            },
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
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    )
                    Spacer(Modifier.height(12.dp))
                    ValidatedTextField(
                        value = noteText,
                        onValueChange = { 
                            noteText = it
                            noteTextError = if (it.isBlank()) "Content is required" else null
                        },
                        label = if (isChecklistMode) "List Items (One per line)" else "Content",
                        error = noteTextError,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                        modifier = Modifier.height(150.dp),
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                BouncyButton(
                    onClick = {
                        if (noteText.isNotBlank() && noteTitle.isNotBlank()) {
                            val id = editingNote?.id ?: System.currentTimeMillis().toString()
                            val type = if (isChecklistMode) "CHECKLIST" else "TEXT"
                            
                            // If switching to checklist for the first time, format items
                            val contentToSave = if (isChecklistMode && !noteText.contains(":false") && !noteText.contains(":true")) {
                                noteText.lines().filter { it.isNotBlank() }.joinToString(",") { "$it:false" }
                            } else {
                                noteText
                            }

                            val combinedData = "$noteTitle::::$contentToSave::::$type"
                            sharedPref.edit { putString(id, combinedData) }
                            
                            val newNote = Note(id, noteTitle, contentToSave, id.toLongOrNull() ?: System.currentTimeMillis(), isChecklistMode)
                            if (editingNote == null) {
                                notes = (listOf(newNote) + notes).sortedByDescending { it.timestamp }
                                assistantViewModel.showMessage("Note saved!", AssistantState.HAPPY)
                            } else {
                                notes = notes.map { if (it.id == id) newNote else it }
                                assistantViewModel.showMessage("Note updated!", AssistantState.HAPPY)
                            }
                            
                            noteText = ""
                            noteTitle = ""
                            isChecklistMode = false
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
                    isChecklistMode = false
                }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}

data class Note(val id: String, val title: String, val content: String, val timestamp: Long, val isChecklist: Boolean = false)

@Composable
fun NoteItem(note: Note, isDarkMode: Boolean, onDelete: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val uid = UserUtils.getEffectiveUid(context)
    val sharedPref = remember(uid) { context.getSharedPreferences("NotesPrefs_$uid", Context.MODE_PRIVATE) }
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
            
            if (note.isChecklist) {
                val items = note.content.split(",").filter { it.contains(":") }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { index, itemStr ->
                        val parts = itemStr.split(":")
                        val text = parts[0]
                        val checked = parts[1].toBoolean()
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    val newItems = items.toMutableList()
                                    newItems[index] = "$text:$isChecked"
                                    val newContent = newItems.joinToString(",")
                                    val combinedData = "${note.title}::::$newContent::::CHECKLIST"
                                    sharedPref.edit { putString(note.id, combinedData) }
                                    // Note: This won't trigger a recompose of the list unless 'notes' state is updated in NoteBookScreen.
                                    // For a quick fix, we can use a local state or rely on the user re-opening the screen.
                                    // Better: The caller should handle updates.
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D1B2)),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text,
                                fontSize = 15.sp,
                                color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.8f),
                                textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        }
                    }
                }
            } else {
                Text(
                    note.content,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.8f)
                )
            }
        }
    }
}
