package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dateMillis: Long,
    val paymentMethod: String,
    val notes: String = ""
) {
    val categoryEnum: ExpenseCategory
        get() = ExpenseCategory.fromString(category)

    val paymentMethodEnum: PaymentMethod
        get() = PaymentMethod.fromString(paymentMethod)
}
