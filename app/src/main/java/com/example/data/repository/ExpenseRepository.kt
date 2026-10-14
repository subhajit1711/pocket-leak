package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val preferencesRepository: UserPreferencesRepository
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val monthlyBudget = preferencesRepository.monthlyBudget
    val leakThreshold = preferencesRepository.leakThreshold
    val themeMode = preferencesRepository.themeMode

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun insertExpenses(expenses: List<Expense>): List<Long> {
        return expenseDao.insertExpenses(expenses)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun clearAllData() {
        expenseDao.clearAllExpenses()
        preferencesRepository.clearAllPreferences()
    }

    fun setMonthlyBudget(budget: Double) {
        preferencesRepository.setMonthlyBudget(budget)
    }

    fun setLeakThreshold(threshold: Double) {
        preferencesRepository.setLeakThreshold(threshold)
    }

    fun setThemeMode(mode: String) {
        preferencesRepository.setThemeMode(mode)
    }

    fun exportToCsv(expenses: List<Expense>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Date,Title,Category,Amount,Payment Method,Notes\n")
        for (expense in expenses) {
            val dateStr = dateFormat.format(Date(expense.dateMillis))
            val cleanTitle = escapeCsv(expense.title)
            val cleanCategory = escapeCsv(expense.category)
            val cleanPayment = escapeCsv(expense.paymentMethod)
            val cleanNotes = escapeCsv(expense.notes)
            sb.append("${expense.id},\"$dateStr\",$cleanTitle,$cleanCategory,${expense.amount},$cleanPayment,$cleanNotes\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun exportToJson(expenses: List<Expense>): String {
        val root = JSONObject()
        root.put("app", "Pocket Leak")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("monthlyBudget", monthlyBudget.value)
        root.put("leakThreshold", leakThreshold.value)

        val expensesArray = JSONArray()
        for (expense in expenses) {
            val item = JSONObject()
            item.put("id", expense.id)
            item.put("title", expense.title)
            item.put("amount", expense.amount)
            item.put("category", expense.category)
            item.put("dateMillis", expense.dateMillis)
            item.put("paymentMethod", expense.paymentMethod)
            item.put("notes", expense.notes)
            expensesArray.put(item)
        }
        root.put("expenses", expensesArray)
        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            if (root.has("monthlyBudget")) {
                val budget = root.getDouble("monthlyBudget")
                if (budget > 0) setMonthlyBudget(budget)
            }
            if (root.has("leakThreshold")) {
                val threshold = root.getDouble("leakThreshold")
                if (threshold > 0) setLeakThreshold(threshold)
            }

            val expensesArray = root.getJSONArray("expenses")
            val importedList = mutableListOf<Expense>()
            for (i in 0 until expensesArray.length()) {
                val item = expensesArray.getJSONObject(i)
                val expense = Expense(
                    id = 0, // auto-generate new ids
                    title = item.optString("title", "Expense"),
                    amount = item.optDouble("amount", 0.0),
                    category = item.optString("category", "Other"),
                    dateMillis = item.optLong("dateMillis", System.currentTimeMillis()),
                    paymentMethod = item.optString("paymentMethod", "Cash"),
                    notes = item.optString("notes", "")
                )
                if (expense.amount > 0) {
                    importedList.add(expense)
                }
            }
            if (importedList.isNotEmpty()) {
                expenseDao.insertExpenses(importedList)
            }
            Result.success(importedList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
