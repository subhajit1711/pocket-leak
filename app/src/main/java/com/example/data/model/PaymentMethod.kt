package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.vector.ImageVector

enum class PaymentMethod(
    val displayName: String,
    val icon: ImageVector
) {
    CASH("Cash", Icons.Filled.Payments),
    UPI("UPI", Icons.Filled.QrCode2),
    CARD("Card", Icons.Filled.CreditCard),
    OTHER("Other", Icons.Filled.MoreHoriz);

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: CASH
        }
    }
}
