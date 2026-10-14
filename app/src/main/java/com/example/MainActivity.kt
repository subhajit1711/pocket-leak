package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.BudgetEditDialog
import com.example.ui.components.ExportBackupDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    EXPENSES("Expenses", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    INSIGHTS("Leak Insights", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val expenseViewModel: ExpenseViewModel = viewModel()
            val themeMode by expenseViewModel.themeMode.collectAsStateWithLifecycle()

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                PocketLeakApp(viewModel = expenseViewModel, currentThemeMode = themeMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketLeakApp(
    viewModel: ExpenseViewModel,
    currentThemeMode: String
) {
    var currentTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    // Dialog visibility states
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showExportBackupDialog by remember { mutableStateOf(false) }

    // Collect UI state
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayTotal.collectAsStateWithLifecycle()
    val thisMonthTotal by viewModel.thisMonthTotal.collectAsStateWithLifecycle()
    val thisYearTotal by viewModel.thisYearTotal.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val leakThreshold by viewModel.leakThreshold.collectAsStateWithLifecycle()
    val biggestExpense by viewModel.biggestExpense.collectAsStateWithLifecycle()
    val totalTransactionCount by viewModel.totalTransactionCount.collectAsStateWithLifecycle()
    val categoryBreakdowns by viewModel.categoryBreakdowns.collectAsStateWithLifecycle()
    val leakInsights by viewModel.leakInsights.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    // Handle system back navigation
    BackHandler(enabled = currentTab != NavigationTab.DASHBOARD) {
        currentTab = NavigationTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pocket Leak",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    // Quick theme toggle
                    IconButton(
                        onClick = {
                            val nextMode = when (currentThemeMode) {
                                "LIGHT" -> "DARK"
                                "DARK" -> "SYSTEM"
                                else -> "LIGHT"
                            }
                            viewModel.setThemeMode(nextMode)
                        },
                        modifier = Modifier.testTag("quick_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (currentThemeMode == "DARK") Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }

                    // Quick export / backup
                    IconButton(
                        onClick = { showExportBackupDialog = true },
                        modifier = Modifier.testTag("quick_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Export / Backup"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_${tab.name}")
                    )
                }
            }
        },
        floatingActionButton = {
            // Show + Add Expense FAB on Dashboard and Expenses tabs
            if (currentTab == NavigationTab.DASHBOARD || currentTab == NavigationTab.EXPENSES) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddDialog() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_expense_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp) // responsive container for tablets/large displays
            ) {
                when (currentTab) {
                    NavigationTab.DASHBOARD -> DashboardScreen(
                        todayTotal = todayTotal,
                        thisMonthTotal = thisMonthTotal,
                        thisYearTotal = thisYearTotal,
                        monthlyBudget = monthlyBudget,
                        biggestExpense = biggestExpense,
                        totalTransactionCount = totalTransactionCount,
                        categoryBreakdowns = categoryBreakdowns,
                        leakInsights = leakInsights,
                        leakThreshold = leakThreshold,
                        recentExpenses = allExpenses,
                        onEditBudgetClick = { showBudgetDialog = true },
                        onThresholdChange = { viewModel.setLeakThreshold(it) },
                        onEditExpense = { viewModel.openEditDialog(it) },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onNavigateToExpenses = { currentTab = NavigationTab.EXPENSES }
                    )

                    NavigationTab.EXPENSES -> ExpensesScreen(
                        expenses = filteredExpenses,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        dateFilter = dateFilter,
                        sortOrder = sortOrder,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryChange = { viewModel.setSelectedCategory(it) },
                        onDateFilterChange = { viewModel.setDateFilter(it) },
                        onSortOrderChange = { viewModel.setSortOrder(it) },
                        onEditExpense = { viewModel.openEditDialog(it) },
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )

                    NavigationTab.INSIGHTS -> InsightsScreen(
                        leakInsights = leakInsights,
                        leakThreshold = leakThreshold,
                        onThresholdChange = { viewModel.setLeakThreshold(it) },
                        allExpenses = allExpenses
                    )

                    NavigationTab.SETTINGS -> SettingsScreen(
                        monthlyBudget = monthlyBudget,
                        themeMode = currentThemeMode,
                        onEditBudgetClick = { showBudgetDialog = true },
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        onOpenExportBackup = { showExportBackupDialog = true }
                    )
                }
            }
        }
    }

    // Add / Edit Expense Dialog
    AddEditExpenseDialog(
        formState = formState,
        onTitleChange = { viewModel.updateFormTitle(it) },
        onAmountChange = { viewModel.updateFormAmount(it) },
        onCategoryChange = { viewModel.updateFormCategory(it) },
        onDateChange = { viewModel.updateFormDate(it) },
        onPaymentMethodChange = { viewModel.updateFormPaymentMethod(it) },
        onNotesChange = { viewModel.updateFormNotes(it) },
        onSave = { viewModel.saveFormExpense() },
        onDismiss = { viewModel.closeFormDialog() }
    )

    // Budget Edit Dialog
    if (showBudgetDialog) {
        BudgetEditDialog(
            currentBudget = monthlyBudget,
            onSaveBudget = { viewModel.setMonthlyBudget(it) },
            onDismiss = { showBudgetDialog = false }
        )
    }

    // Export & Backup Dialog
    ExportBackupDialog(
        isOpen = showExportBackupDialog,
        onGetCsv = { viewModel.getExportCsv() },
        onGetJson = { viewModel.getExportJson() },
        onImportJson = { viewModel.importJsonData(it) },
        onClearAllData = { viewModel.clearAllData() },
        onDismiss = { showExportBackupDialog = false }
    )
}
