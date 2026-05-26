package com.enosh.fincalc.domain.model

import androidx.compose.ui.graphics.Color

data class Tool(val id: String, val name: String, val iconRes: Int)
data class Category(val title: String, val tools: List<Tool>, val color: Color)