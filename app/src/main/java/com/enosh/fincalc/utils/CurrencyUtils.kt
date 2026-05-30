package com.enosh.fincalc.utils

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CurrencyInfo(
    val country: String,
    val code: String,
    val symbol: String,
    val locale: Locale
)

object CurrencyUtils {
    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("UK", "GBP", "£", Locale.UK),
        CurrencyInfo("US", "USD", "$", Locale.US),
        CurrencyInfo("Europe", "EUR", "€", Locale.GERMANY),
        CurrencyInfo("India", "INR", "₹", Locale("en", "IN")),
        CurrencyInfo("Sri Lanka", "LKR", "Rs", Locale("en", "LK")),
        CurrencyInfo("Japan", "JPY", "¥", Locale.JAPAN)
    )

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
