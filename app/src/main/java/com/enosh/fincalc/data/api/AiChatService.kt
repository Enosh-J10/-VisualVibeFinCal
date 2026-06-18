package com.enosh.fincalc.data.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class AiChatService {
    
    fun sendMessage(userId: String, message: String, context: Map<String, Any>): Flow<String> = flow {
        val query = message.lowercase()
        
        // Simulate thinking delay
        delay(500)
        
        val response = when {
            query.contains("date") || query.contains("today") -> {
                val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
                "Today is $date."
            }
            query.contains("who made") || query.contains("developer") || query.contains("author") -> {
                "FinCalc was developed by Enosh as a comprehensive financial management and utility tool. It's designed to help users track expenses, plan budgets, and perform various calculations with ease."
            }
            query.contains("save money") || query.contains("saving tips") -> {
                "Here are some tips to save money:\n1. Track every expense, no matter how small.\n2. Use the 50/30/20 rule: 50% for needs, 30% for wants, and 20% for savings.\n3. Review your subscriptions regularly.\n4. Plan your meals to avoid impulsive dining out.\n5. Use the FinCalc 'Savings Goals' tool to stay motivated!"
            }
            query.contains("inflation") -> {
                "Inflation is the rate at which the general level of prices for goods and services is rising, and, consequently, the purchasing power of currency is falling. It's important to invest your savings to beat inflation over the long term."
            }
            query.contains("compound interest") -> {
                "Compound interest is interest calculated on the initial principal, which also includes all of the accumulated interest from previous periods on a deposit or loan. It's often called the 'eighth wonder of the world' because of how it grows wealth over time."
            }
            query.contains("smart business") -> {
                "The Smart Business tool allows you to track your income sources, set monthly targets, and monitor your progress. You can categorize income by Services, Repairs, Sales, etc., and see which payment methods are most used."
            }
            query.contains("smart travel") -> {
                "Smart Travel helps you manage trip expenses, split costs with friends, and handle settlements. It's perfect for group tours where you need to track who paid for what."
            }
            query.contains("budget") -> {
                "In FinCalc, you can set a base monthly budget and add 'Extra' amounts for unexpected income. The Budget Planner will show you how much you've spent versus your available budget."
            }
            query.contains("hello") || query.contains("hi") -> {
                "Hello! I'm your FinCalc Assistant. How can I help you with your finances today?"
            }
            else -> {
                "That's an interesting question! While I'm still learning some specifics, I can help you with budgeting, saving tips, and explaining financial concepts like inflation or compound interest. Feel free to ask about any of the tools in FinCalc!"
            }
        }

        // Simulate streaming effect
        val words = response.split(" ")
        var currentResponse = ""
        for (word in words) {
            currentResponse += if (currentResponse.isEmpty()) word else " $word"
            emit(currentResponse)
            delay(50) // Adjust for streaming speed
        }
    }
}
