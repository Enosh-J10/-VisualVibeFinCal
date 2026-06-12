package com.enosh.fincalc.utils

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CurrencyInfo(
    val country: String,
    val flag: String,
    val code: String,
    val symbol: String,
    val locale: Locale
)

object CurrencyUtils {
    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("United Kingdom", "🇬🇧", "GBP", "£", Locale.UK),
        CurrencyInfo("United States", "🇺🇸", "USD", "$", Locale.US),
        CurrencyInfo("Europe", "🇪🇺", "EUR", "€", Locale.GERMANY),
        CurrencyInfo("India", "🇮🇳", "INR", "₹", Locale("en", "IN")),
        CurrencyInfo("Sri Lanka", "🇱🇰", "LKR", "Rs", Locale("en", "LK")),
        CurrencyInfo("Japan", "🇯🇵", "JPY", "¥", Locale.JAPAN),
        CurrencyInfo("Canada", "🇨🇦", "CAD", "$", Locale.CANADA),
        CurrencyInfo("Australia", "🇦🇺", "AUD", "$", Locale("en", "AU")),
        CurrencyInfo("China", "🇨🇳", "CNY", "¥", Locale.CHINA),
        CurrencyInfo("Brazil", "🇧🇷", "BRL", "R$", Locale("pt", "BR")),
        CurrencyInfo("South Africa", "🇿🇦", "ZAR", "R", Locale("en", "ZA")),
        CurrencyInfo("United Arab Emirates", "🇦🇪", "AED", "د.إ", Locale("ar", "AE")),
        CurrencyInfo("Switzerland", "🇨🇭", "CHF", "CHf", Locale("de", "CH")),
        CurrencyInfo("Singapore", "🇸🇬", "SGD", "$", Locale("en", "SG")),
        CurrencyInfo("Mexico", "🇲🇽", "MXN", "$", Locale("es", "MX"))
    ).sortedBy { it.country }

    private const val PREFS_NAME = "UserPrefs"
    private const val KEY_CURRENCY_CODE = "default_currency_code"

    fun getSelectedCurrency(context: Context): CurrencyInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_CURRENCY_CODE, "GBP") ?: "GBP"
        return SUPPORTED_CURRENCIES.find { it.code == code } ?: SUPPORTED_CURRENCIES[0]
    }

    fun setSelectedCurrency(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY_CODE, code).apply()
    }

    fun formatCurrency(context: Context, amount: Double): String {
        val currencyInfo = getSelectedCurrency(context)
        return try {
            val format = NumberFormat.getCurrencyInstance(currencyInfo.locale)
            format.currency = Currency.getInstance(currencyInfo.code)
            format.format(amount)
        } catch (e: Exception) {
            "${currencyInfo.symbol}${String.format("%.2f", amount)}"
        }
    }
}
