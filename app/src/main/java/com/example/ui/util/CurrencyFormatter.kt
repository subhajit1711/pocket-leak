package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val indianLocale = Locale("en", "IN")

    fun formatInr(amount: Double, includeDecimals: Boolean = false): String {
        val format = NumberFormat.getCurrencyInstance(indianLocale)
        format.maximumFractionDigits = if (includeDecimals) 2 else 0
        format.minimumFractionDigits = if (includeDecimals) 2 else 0
        val formatted = format.format(amount)
        // Ensure proper Rupee symbol
        return if (!formatted.startsWith("₹")) {
            "₹" + formatted.replace("INR", "").replace("Rs.", "").trim()
        } else {
            formatted
        }
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateShort(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
