package com.enosh.fincalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enosh.fincalc.ui.theme.FinCalcTheme
import com.enosh.fincalc.utils.UserUtils

class TermsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPref = getSharedPreferences(UserUtils.PREFS_NAME, MODE_PRIVATE)
        val uid = UserUtils.getEffectiveUid(this)
        val darkModeKey = UserUtils.getScopedKey(uid, "is_dark_mode")
        val isDarkMode = sharedPref.getBoolean(darkModeKey, true)

        setContent {
            FinCalcTheme(darkTheme = isDarkMode) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Legal Information") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            windowInsets = WindowInsets.statusBars
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val content = stringResource(R.string.terms_and_conditions_text)
                    val sections = content.split("\n\n").filter { it.isNotBlank() }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 16.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        sections.forEach { section ->
                            item {
                                LegalSection(section.trim())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegalSection(text: String) {
    val lines = text.split("\n")
    Column {
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEachIndexed
            
            if (index == 0) {
                val isMainHeading = trimmedLine == "Terms & Conditions" || trimmedLine == "Privacy Policy"
                Text(
                    text = trimmedLine,
                    fontSize = if (isMainHeading) 28.sp else 20.sp,
                    fontWeight = if (isMainHeading) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isMainHeading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    lineHeight = if (isMainHeading) 36.sp else 28.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                Text(
                    text = trimmedLine,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
