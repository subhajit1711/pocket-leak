package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExpenseDao
import com.example.data.local.PocketLeakDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.Expense
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: ExpenseRepository
    private lateinit var dao: ExpenseDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val db = PocketLeakDatabase.getDatabase(context)
        dao = db.expenseDao()
        val prefs = UserPreferencesRepository(context)
        repository = ExpenseRepository(dao, prefs)
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Pocket Leak", appName)
    }

    @Test
    fun `test expense insertion and csv export`() = runBlocking {
        val expense = Expense(
            title = "Chai",
            amount = 60.0,
            category = "Food",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = "UPI",
            notes = "Daily chai leak"
        )
        val id = repository.insertExpense(expense)
        assertTrue(id > 0)

        val all = repository.allExpenses.first()
        assertTrue(all.any { it.title == "Chai" })

        val csv = repository.exportToCsv(all)
        assertTrue(csv.contains("Chai"))
        assertTrue(csv.contains("Food"))
        assertTrue(csv.contains("UPI"))
    }

    @Test
    fun `test json export and restore`() = runBlocking {
        val expense = Expense(
            title = "Metro recharge",
            amount = 200.0,
            category = "Travel",
            dateMillis = System.currentTimeMillis(),
            paymentMethod = "Card",
            notes = "Work trip"
        )
        repository.insertExpense(expense)
        val all = repository.allExpenses.first()

        val json = repository.exportToJson(all)
        assertTrue(json.contains("Pocket Leak"))
        assertTrue(json.contains("Metro recharge"))

        // Test restore
        val result = repository.importFromJson(json)
        assertTrue(result.isSuccess)
    }
}
