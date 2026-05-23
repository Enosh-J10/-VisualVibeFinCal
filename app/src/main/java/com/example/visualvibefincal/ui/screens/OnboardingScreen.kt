package com.example.visualvibefincal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visualvibefincal.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = listOf(
        OnboardingPage(
            "All your financial tools in one place",
            "Currency conversion, loan calculators, tax tools and more - everything you need to manage your money.",
            R.drawable.ic_fincalc_logo_vector
        ),
        OnboardingPage(
            "Scan receipts and track expenses",
            "Our Smart Scan AI reads your receipts automatically and helps you categorize your spending.",
            R.drawable.ic_calc
        ),
        OnboardingPage(
            "Get smart tips from your assistant",
            "Our AI assistant is here to guide you, provide financial insights, and help you reach your goals.",
            R.drawable.ic_bmi
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(targetState = currentPage, label = "") { pageIndex ->
                val page = pages[pageIndex]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(page.icon),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp),
                        tint = Color(0xFF00D1B2)
                    )
                    Spacer(Modifier.height(48.dp))
                    Text(
                        page.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        page.description,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 12.dp else 8.dp)
                            .background(if (index == currentPage) Color(0xFF00D1B2) else Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onFinished) {
                Text("Skip", color = Color.White.copy(alpha = 0.6f))
            }
            
            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp).width(140.dp)
            ) {
                Text(if (currentPage == pages.size - 1) "Get Started" else "Next", fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class OnboardingPage(val title: String, val description: String, val icon: Int)
