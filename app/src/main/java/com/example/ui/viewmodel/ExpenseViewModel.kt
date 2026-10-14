package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PocketLeakDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year")
}

enum class SortOrder(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_AMOUNT("Highest ₹"),
    LOWEST_AMOUNT("Lowest ₹")
}

data class CategoryBreakdown(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class LeakInsightData(
    val smallExpenses: List<Expense>,
    val totalSmallSpentThisMonth: Double,
    val smallTransactionsCountThisMonth: Int,
    val projectedMonthlyLeak: Double,
    val projectedAnnualLeak: Double,
    val highestCategory: ExpenseCategory?,
    val highestCategoryTotal: Double,
    val highestCategoryPercent: Float,
    val currentMonthTotal: Double,
    val previousMonthTotal: Double,
    val momDifference: Double,
    val momPercentChange: Double?
)

data class ExpenseFormState(
    val id: Long? = null,
    val title: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val notes: String = "",
    val isOpen: Boolean = false,
    val titleError: String? = null,
    val amountError: String? = null
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository

    init {
        val database = PocketLeakDatabase.getDatabase(application)
        val prefs = UserPreferencesRepository(application)
        repository = ExpenseRepository(database.expenseDao(), prefs)

        // Seed realistic starter expenses if empty
        viewModelScope.launch {
            repository.allExpenses.collect { list ->
                if (list.isEmpty()) {
                    seedStarterData()
                }
            }
        }
    }

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyBudget: StateFlow<Double> = repository.monthlyBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25000.0)

    val leakThreshold: StateFlow<Double> = repository.leakThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 300.0)

    val themeMode: StateFlow<String> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    // Filter & Search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter = _dateFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    // Form state
    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState = _formState.asStateFlow()

    // Filtered expenses
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _searchQuery,
        _selectedCategory,
        _dateFilter,
        _sortOrder
    ) { expenses, query, cat, dateFilt, sort ->
        val now = Calendar.getInstance()

        expenses.filter { item ->
            // Search query filter
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.notes.contains(query, ignoreCase = true)

            // Category filter
            val matchesCat = cat == null || item.categoryEnum == cat

            // Date filter
            val matchesDate = when (dateFilt) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> isSameDay(item.dateMillis, now.timeInMillis)
                DateFilter.THIS_WEEK -> isSameWeek(item.dateMillis, now)
                DateFilter.THIS_MONTH -> isSameMonth(item.dateMillis, now)
                DateFilter.THIS_YEAR -> isSameYear(item.dateMillis, now)
            }

            matchesQuery && matchesCat && matchesDate
        }.sortedWith { a, b ->
            when (sort) {
                SortOrder.NEWEST -> b.dateMillis.compareTo(a.dateMillis)
                SortOrder.OLDEST -> a.dateMillis.compareTo(b.dateMillis)
                SortOrder.HIGHEST_AMOUNT -> b.amount.compareTo(a.amount)
                SortOrder.LOWEST_AMOUNT -> a.amount.compareTo(b.amount)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard totals
    val todayTotal: StateFlow<Double> = allExpenses.combine(_searchQuery) { expenses, _ ->
        val today = Calendar.getInstance().timeInMillis
        expenses.filter { isSameDay(it.dateMillis, today) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thisMonthTotal: StateFlow<Double> = allExpenses.combine(_searchQuery) { expenses, _ ->
        val cal = Calendar.getInstance()
        expenses.filter { isSameMonth(it.dateMillis, cal) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val thisYearTotal: StateFlow<Double> = allExpenses.combine(_searchQuery) { expenses, _ ->
        val cal = Calendar.getInstance()
        expenses.filter { isSameYear(it.dateMillis, cal) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val biggestExpense: StateFlow<Expense?> = allExpenses.combine(_searchQuery) { expenses, _ ->
        expenses.maxByOrNull { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalTransactionCount: StateFlow<Int> = allExpenses.combine(_searchQuery) { expenses, _ ->
        expenses.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Category Breakdowns (for current month or all time)
    val categoryBreakdowns: StateFlow<List<CategoryBreakdown>> = allExpenses.combine(_searchQuery) { expenses, _ ->
        val cal = Calendar.getInstance()
        val monthExpenses = expenses.filter { isSameMonth(it.dateMillis, cal) }
        val targetList = if (monthExpenses.isNotEmpty()) monthExpenses else expenses
        val total = targetList.sumOf { it.amount }

        if (total == 0.0) {
            emptyList()
        } else {
            ExpenseCategory.entries.mapNotNull { cat ->
                val items = targetList.filter { it.categoryEnum == cat }
                val catSum = items.sumOf { it.amount }
                if (catSum > 0) {
                    CategoryBreakdown(
                        category = cat,
                        totalAmount = catSum,
                        percentage = (catSum / total).toFloat(),
                        transactionCount = items.size
                    )
                } else null
            }.sortedByDescending { it.totalAmount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Leak insights
    val leakInsights: StateFlow<LeakInsightData> = combine(
        allExpenses,
        leakThreshold,
        monthlyBudget
    ) { expenses, threshold, _ ->
        calculateLeakInsights(expenses, threshold)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LeakInsightData(
            smallExpenses = emptyList(),
            totalSmallSpentThisMonth = 0.0,
            smallTransactionsCountThisMonth = 0,
            projectedMonthlyLeak = 0.0,
            projectedAnnualLeak = 0.0,
            highestCategory = null,
            highestCategoryTotal = 0.0,
            highestCategoryPercent = 0f,
            currentMonthTotal = 0.0,
            previousMonthTotal = 0.0,
            momDifference = 0.0,
            momPercentChange = null
        )
    )

    private fun calculateLeakInsights(expenses: List<Expense>, threshold: Double): LeakInsightData {
        val now = Calendar.getInstance()
        val thisMonthExpenses = expenses.filter { isSameMonth(it.dateMillis, now) }

        // Previous month calculation
        val prevMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonthExpenses = expenses.filter { isSameMonth(it.dateMillis, prevMonthCal) }

        val currentMonthTotal = thisMonthExpenses.sumOf { it.amount }
        val prevMonthTotal = prevMonthExpenses.sumOf { it.amount }

        val momDiff = currentMonthTotal - prevMonthTotal
        val momPercent = if (prevMonthTotal > 0) {
            ((currentMonthTotal - prevMonthTotal) / prevMonthTotal) * 100
        } else null

        // Small repeated expenses (Pocket Leaks: expenses <= threshold)
        val smallThisMonth = thisMonthExpenses.filter { it.amount <= threshold }
        val smallAll = expenses.filter { it.amount <= threshold }

        val totalSmallThisMonth = smallThisMonth.sumOf { it.amount }
        val countSmallThisMonth = smallThisMonth.size

        // Calculate projected monthly and annual leak:
        // If current month has data, project across current day of month
        val currentDay = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val projectedMonthly = if (totalSmallThisMonth > 0) {
            (totalSmallThisMonth / currentDay) * daysInMonth
        } else {
            // fallback to average of all small expenses
            smallAll.sumOf { it.amount }
        }
        val projectedAnnual = projectedMonthly * 12

        // Highest spending category in this month (or all time if month empty)
        val baseExpenses = if (thisMonthExpenses.isNotEmpty()) thisMonthExpenses else expenses
        val baseTotal = baseExpenses.sumOf { it.amount }
        val categoryTotals = ExpenseCategory.entries.map { cat ->
            cat to baseExpenses.filter { it.categoryEnum == cat }.sumOf { it.amount }
        }.sortedByDescending { it.second }

        val topCategory = categoryTotals.firstOrNull { it.second > 0 }
        val highestCat = topCategory?.first
        val highestCatTotal = topCategory?.second ?: 0.0
        val highestCatPercent = if (baseTotal > 0) (highestCatTotal / baseTotal).toFloat() else 0f

        return LeakInsightData(
            smallExpenses = smallThisMonth,
            totalSmallSpentThisMonth = totalSmallThisMonth,
            smallTransactionsCountThisMonth = countSmallThisMonth,
            projectedMonthlyLeak = projectedMonthly,
            projectedAnnualLeak = projectedAnnual,
            highestCategory = highestCat,
            highestCategoryTotal = highestCatTotal,
            highestCategoryPercent = highestCatPercent,
            currentMonthTotal = currentMonthTotal,
            previousMonthTotal = prevMonthTotal,
            momDifference = momDiff,
            momPercentChange = momPercent
        )
    }

    // Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: ExpenseCategory?) {
        _selectedCategory.value = category
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setMonthlyBudget(budget: Double) {
        repository.setMonthlyBudget(budget)
    }

    fun setLeakThreshold(threshold: Double) {
        repository.setLeakThreshold(threshold)
    }

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
    }

    // Form operations
    fun openAddDialog() {
        _formState.value = ExpenseFormState(
            id = null,
            title = "",
            amount = "",
            category = ExpenseCategory.FOOD,
            dateMillis = System.currentTimeMillis(),
            paymentMethod = PaymentMethod.UPI,
            notes = "",
            isOpen = true
        )
    }

    fun openEditDialog(expense: Expense) {
        _formState.value = ExpenseFormState(
            id = expense.id,
            title = expense.title,
            amount = if (expense.amount % 1.0 == 0.0) expense.amount.toLong().toString() else expense.amount.toString(),
            category = expense.categoryEnum,
            dateMillis = expense.dateMillis,
            paymentMethod = expense.paymentMethodEnum,
            notes = expense.notes,
            isOpen = true
        )
    }

    fun closeFormDialog() {
        _formState.value = _formState.value.copy(isOpen = false)
    }

    fun updateFormTitle(title: String) {
        _formState.value = _formState.value.copy(title = title, titleError = null)
    }

    fun updateFormAmount(amount: String) {
        _formState.value = _formState.value.copy(amount = amount, amountError = null)
    }

    fun updateFormCategory(category: ExpenseCategory) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun updateFormDate(dateMillis: Long) {
        _formState.value = _formState.value.copy(dateMillis = dateMillis)
    }

    fun updateFormPaymentMethod(method: PaymentMethod) {
        _formState.value = _formState.value.copy(paymentMethod = method)
    }

    fun updateFormNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun saveFormExpense(): Boolean {
        val state = _formState.value
        val titleTrimmed = state.title.trim()
        val amountDouble = state.amount.toDoubleOrNull()

        var hasError = false
        var titleErr: String? = null
        var amountErr: String? = null

        if (titleTrimmed.isBlank()) {
            titleErr = "Please enter an expense name"
            hasError = true
        }

        if (amountDouble == null || amountDouble <= 0) {
            amountErr = "Enter a valid amount (> 0)"
            hasError = true
        }

        if (hasError) {
            _formState.value = state.copy(titleError = titleErr, amountError = amountErr)
            return false
        }

        viewModelScope.launch {
            val expense = Expense(
                id = state.id ?: 0,
                title = titleTrimmed,
                amount = amountDouble!!,
                category = state.category.displayName,
                dateMillis = state.dateMillis,
                paymentMethod = state.paymentMethod.displayName,
                notes = state.notes.trim()
            )

            if (state.id == null || state.id == 0L) {
                repository.insertExpense(expense)
            } else {
                repository.updateExpense(expense)
            }
        }

        _formState.value = state.copy(isOpen = false)
        return true
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun getExportCsv(): String {
        return repository.exportToCsv(allExpenses.value)
    }

    fun getExportJson(): String {
        return repository.exportToJson(allExpenses.value)
    }

    suspend fun importJsonData(json: String): Result<Int> {
        return repository.importFromJson(json)
    }

    private suspend fun seedStarterData() {
        val now = System.currentTimeMillis()
        val day = 24L * 60 * 60 * 1000
        val starterList = listOf(
            Expense(title = "Chai & Morning Samosa", amount = 60.0, category = "Food", dateMillis = now - (day * 0), paymentMethod = "UPI", notes = "Daily office tea leak"),
            Expense(title = "Metro SmartCard Recharge", amount = 500.0, category = "Travel", dateMillis = now - (day * 1), paymentMethod = "UPI", notes = "Monthly commute recharge"),
            Expense(title = "Evening Coffee with Friends", amount = 280.0, category = "Food", dateMillis = now - (day * 1), paymentMethod = "UPI", notes = "Café latte"),
            Expense(title = "D-Mart Monthly Grocery", amount = 3450.0, category = "Shopping", dateMillis = now - (day * 2), paymentMethod = "Card", notes = "Pantry essentials"),
            Expense(title = "Auto Rickshaw Fare", amount = 95.0, category = "Travel", dateMillis = now - (day * 2), paymentMethod = "Cash", notes = "Return from station"),
            Expense(title = "Broadband Wi-Fi Fiber", amount = 999.0, category = "Bills", dateMillis = now - (day * 4), paymentMethod = "UPI", notes = "High speed plan"),
            Expense(title = "Online Skills Course", amount = 1499.0, category = "Education", dateMillis = now - (day * 6), paymentMethod = "Card", notes = "Kotlin Android Masterclass"),
            Expense(title = "Weekend Cinema & Popcorn", amount = 720.0, category = "Entertainment", dateMillis = now - (day * 7), paymentMethod = "UPI", notes = "IMAX 3D show"),
            Expense(title = "Vitamin Supplements & Meds", amount = 480.0, category = "Health", dateMillis = now - (day * 8), paymentMethod = "Card", notes = "Pharmacy store"),
            Expense(title = "Chai & Biscuits", amount = 45.0, category = "Food", dateMillis = now - (day * 9), paymentMethod = "Cash", notes = "Corner stall leak"),
            Expense(title = "Packaged Water Bottle", amount = 20.0, category = "Food", dateMillis = now - (day * 10), paymentMethod = "UPI", notes = "Travel leak")
        )
        repository.insertExpenses(starterList)
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(time: Long, now: Calendar): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            c.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(time: Long, now: Calendar): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            c.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }

    private fun isSameYear(time: Long, now: Calendar): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    }
}
