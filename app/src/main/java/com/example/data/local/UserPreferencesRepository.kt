package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pocket_leak_preferences", Context.MODE_PRIVATE)

    private val _monthlyBudget = MutableStateFlow(getSavedBudget())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _leakThreshold = MutableStateFlow(getSavedLeakThreshold())
    val leakThreshold: StateFlow<Double> = _leakThreshold.asStateFlow()

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private fun getSavedBudget(): Double {
        return prefs.getString("monthly_budget", "25000.0")?.toDoubleOrNull() ?: 25000.0
    }

    private fun getSavedLeakThreshold(): Double {
        return prefs.getString("leak_threshold", "300.0")?.toDoubleOrNull() ?: 300.0
    }

    private fun getSavedThemeMode(): String {
        return prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    }

    fun setMonthlyBudget(budget: Double) {
        prefs.edit().putString("monthly_budget", budget.toString()).apply()
        _monthlyBudget.value = budget
    }

    fun setLeakThreshold(threshold: Double) {
        prefs.edit().putString("leak_threshold", threshold.toString()).apply()
        _leakThreshold.value = threshold
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        _monthlyBudget.value = 25000.0
        _leakThreshold.value = 300.0
        _themeMode.value = "SYSTEM"
    }
}
