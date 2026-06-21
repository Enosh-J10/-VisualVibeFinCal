package com.enosh.fincalc.data.model

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList()
)

data class ChecklistItem(
    val text: String = "",
    val checked: Boolean = false
)
