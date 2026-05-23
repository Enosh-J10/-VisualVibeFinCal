package com.example.visualvibefincal.utils

import android.util.Patterns

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.isNotBlank()
    }

    fun isValidNumeric(input: String): Boolean {
        if (input.isBlank()) return false
        val d = input.toDoubleOrNull()
        return d != null && !d.isInfinite() && !d.isNaN()
    }

    fun isValidPositiveNumeric(input: String): Boolean {
        if (input.isBlank()) return false
        val d = input.toDoubleOrNull()
        return d != null && d > 0 && !d.isInfinite() && !d.isNaN()
    }

    fun formatNumericInput(input: String, allowNegative: Boolean = true): String {
        // Only allow digits, dots, and minus signs
        var filtered = input.filter { it.isDigit() || it == '.' || it == '-' }
        
        // Handle negative sign
        if (!allowNegative) {
            filtered = filtered.replace("-", "")
        } else if (filtered.count { it == '-' } > 1 || (filtered.contains('-') && filtered.indexOf('-') != 0)) {
            // Only allow a minus sign at the very beginning
            val hasLeadingMinus = filtered.startsWith("-")
            filtered = (if (hasLeadingMinus) "-" else "") + filtered.replace("-", "")
        }

        val parts = filtered.split('.')
        return if (parts.size > 2) {
            parts[0] + "." + parts[1]
        } else {
            filtered
        }
    }
}
