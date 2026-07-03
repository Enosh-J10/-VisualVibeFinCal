package com.enosh.fincalc.utils

enum class ReminderCategory(val label: String, val navigateTo: String) {
    FRIEND_REQUEST("Friend Request", "friends_pending"),
    ADD_FRIEND("Add Friend", "friends_add"),
    EXPENSE("Expense Tracking", "expenses"),
    BUDGET("Budget Planner", "budget"),
    SMART_TRAVEL("Smart Travel", "smart_travel"),
    SMART_BUSINESS("Smart Business", "smart_business"),
    NOTES("Notes", "notes"),
    UNIT_CONVERTER("Unit Converter", "unit")
}

object ReminderMessages {
    private val normalMessages = mapOf(
        ReminderCategory.FRIEND_REQUEST to listOf(
            "👀 Someone might be waiting… check your friend requests!",
            "Your future finance buddy could be one tap away."
        ),
        ReminderCategory.ADD_FRIEND to listOf(
            "🤝 Add a friend and start tracking smarter together.",
            "FinCalc is more fun with friends. Go find one."
        ),
        ReminderCategory.EXPENSE to listOf(
            "💸 Spent something today? Track it before your wallet starts lying.",
            "Don’t trust memory. Track today’s expenses."
        ),
        ReminderCategory.BUDGET to listOf(
            "📊 Quick budget check? Future you will thank you.",
            "Your budget wants attention. Don’t ghost it."
        ),
        ReminderCategory.SMART_TRAVEL to listOf(
            "✈️ Planning a trip? Split expenses before chaos begins.",
            "Group trips are fun until someone forgets who paid."
        ),
        ReminderCategory.SMART_BUSINESS to listOf(
            "💼 Any income today? Log it like a boss.",
            "Your business numbers won’t track themselves."
        ),
        ReminderCategory.NOTES to listOf(
            "📝 Got something to remember? Drop it in Notes.",
            "Brain full? FinCalc Notes exists for a reason."
        ),
        ReminderCategory.UNIT_CONVERTER to listOf(
            "📏 Converting something? FinCalc is ready.",
            "Quick unit swap? No problem."
        )
    )

    private val roastMessages = mapOf(
        ReminderCategory.FRIEND_REQUEST to listOf(
            "👀 Still waiting for friends? Maybe check your requests first.",
            "Add some friends. Even your budget needs witnesses."
        ),
        ReminderCategory.ADD_FRIEND to listOf(
            "🤝 FinCalc is better with friends. Assuming you have any. Just kidding, add some!",
            "Stop being a lone wolf and add a finance buddy."
        ),
        ReminderCategory.EXPENSE to listOf(
            "💸 Track your expenses before your bank account files a complaint.",
            "🧾 Forgot to track spending again? Classic."
        ),
        ReminderCategory.BUDGET to listOf(
            "📊 Your budget is crying quietly. Go check it.",
            "Budgeting is hard. Ignoring it is easy. Which one are you doing?"
        ),
        ReminderCategory.SMART_TRAVEL to listOf(
            "✈️ Planning a trip? Try not to spend it all on day one.",
            "Traveling is fun. Arguments about who paid for lunch aren't. Split now."
        ),
        ReminderCategory.SMART_BUSINESS to listOf(
            "💼 Logging income? Or is it all out and no in today?",
            "Numbers don't lie. Unlike your 'business lunch' excuses."
        ),
        ReminderCategory.NOTES to listOf(
            "📝 Use the Notes. Your memory is clearly failing you.",
            "I'd suggest you write this down, but you'll probably forget where."
        ),
        ReminderCategory.UNIT_CONVERTER to listOf(
            "📏 Unit conversion: for when you're too confused to do the math yourself.",
            "Need to swap units? Again? Fine, I'll help."
        )
    )

    fun getRandomMessage(category: ReminderCategory, isRoastMode: Boolean): String {
        val messages = if (isRoastMode) roastMessages[category] else normalMessages[category]
        return messages?.random() ?: "Time to check FinCalc!"
    }
}
