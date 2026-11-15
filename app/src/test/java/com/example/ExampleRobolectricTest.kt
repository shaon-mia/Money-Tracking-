package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmConfigState
import com.example.alarm.AlarmScheduler
import com.example.alarm.TextAlarmItem
import com.example.data.model.DefaultCatalog
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MoneyTracker", appName)
  }

  @Test
  fun `default categories include Others in income and expense`() {
    assertTrue(DefaultCatalog.incomeCategories.any { it.id == "inc_other" && it.nameEn == "Others" })
    assertTrue(DefaultCatalog.expenseCategories.any { it.id == "exp_other" && it.nameEn == "Others" })
  }

  @Test
  fun `alarm and text alarm persistence round trip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val state = AlarmConfigState(
      dailyEnabled = true,
      dailyHour = 21,
      dailyMinute = 30,
      dailyText = "আজকের খরচ লিখে রাখুন",
      textAlarms = listOf(
        TextAlarmItem(
          id = "txt_1",
          text = "বিদ্যুৎ বিল দিন",
          hour = 10,
          minute = 15,
          repeatDaily = true,
          enabled = true
        )
      )
    )
    AlarmScheduler.saveAndScheduleAll(context, state)
    val loaded = AlarmScheduler.loadState(context)
    assertTrue(loaded.dailyEnabled)
    assertEquals(21, loaded.dailyHour)
    assertEquals(30, loaded.dailyMinute)
    assertEquals("আজকের খরচ লিখে রাখুন", loaded.dailyText)
    assertEquals(1, loaded.textAlarms.size)
    assertEquals("বিদ্যুৎ বিল দিন", loaded.textAlarms.first().text)

    AlarmScheduler.ensureNotificationChannel(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = nm.getNotificationChannel(AlarmScheduler.CHANNEL_ID)
    assertTrue(channel != null)
    assertTrue(!channel!!.shouldVibrate())
    assertEquals(android.app.NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
  }

  @Test
  fun `storage mode switches cleanly between cloud and offline local`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(com.example.data.repository.StorageModeStore.MODE_CLOUD, com.example.data.repository.StorageModeStore.getMode(context))
    com.example.data.repository.StorageModeStore.setMode(context, com.example.data.repository.StorageModeStore.MODE_LOCAL)
    assertEquals(com.example.data.repository.StorageModeStore.MODE_LOCAL, com.example.data.repository.StorageModeStore.getMode(context))
    com.example.data.repository.StorageModeStore.markLocalMigrated(context, "uid_123")
    assertTrue(com.example.data.repository.StorageModeStore.isLocalMigratedTo(context, "uid_123"))
    com.example.data.repository.StorageModeStore.clearLocalMigratedFlags(context)
    assertTrue(!com.example.data.repository.StorageModeStore.isLocalMigratedTo(context, "uid_123"))
  }

  @Test
  fun `delete expense password preference persists and verifies hash`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ds = com.example.data.repository.LocalFinanceDataSource(context)
    ds.seedInitialDataIfNeeded(com.example.data.repository.LocalFinanceDataSource.LOCAL_USER_ID)
    val hash = com.example.util.ReportExporter.sha256("MySecret99")
    val prefs = com.example.data.model.UserPreferences(
      userId = com.example.data.repository.LocalFinanceDataSource.LOCAL_USER_ID,
      deletePasswordEnabled = true,
      deletePasswordHash = hash,
      demoSeeded = true
    )
    ds.savePreferences(com.example.data.repository.LocalFinanceDataSource.LOCAL_USER_ID, prefs)

    val dsReloaded = com.example.data.repository.LocalFinanceDataSource(context)
    dsReloaded.seedInitialDataIfNeeded(com.example.data.repository.LocalFinanceDataSource.LOCAL_USER_ID)
    val loadedPrefs = dsReloaded.observePreferences(
      com.example.data.repository.LocalFinanceDataSource.LOCAL_USER_ID
    ).first()
    assertTrue(loadedPrefs.deletePasswordEnabled)
    assertEquals(hash, loadedPrefs.deletePasswordHash)
    assertTrue(loadedPrefs.deletePasswordHash != com.example.util.ReportExporter.sha256("WrongPass"))
  }

  @Test
  fun `expanded expense categories and suggested logos work accurately`() {
    val expenseIds = DefaultCatalog.expenseCategories.map { it.id }.toSet()
    assertTrue(expenseIds.size >= 22)
    assertTrue("exp_groceries" in expenseIds)
    assertTrue("exp_breakfast" in expenseIds)
    assertTrue("exp_fuel" in expenseIds)
    assertTrue("exp_clothes" in expenseIds)
    assertTrue("exp_travel" in expenseIds)

    val teaSuggestions = com.example.util.CategoryIconMapper.suggestIcons("চা নাস্তা", "expense").map { it.first }
    assertTrue(teaSuggestions.first() == "cafe")

    val fuelSuggestions = com.example.util.CategoryIconMapper.suggestIcons("bike petrol", "expense").map { it.first }
    assertTrue(fuelSuggestions.contains("local_gas_station"))
    assertTrue(fuelSuggestions.contains("two_wheeler"))
  }

  @Test
  fun `syncMissingDefaultCategories preserves edited category and adds new defaults`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ds = com.example.data.repository.LocalFinanceDataSource(context)
    val userId = "upgrade_test_user"
    ds.seedInitialDataIfNeeded(userId)

    val originalFood = ds.observeCategories(userId).first().first { it.id == "exp_food" }
    val editedFood = originalFood.copy(nameBn = "হোটেল ও খাবার", iconKey = "fastfood")
    ds.upsertCategory(userId, editedFood)

    // Remove one newly added category to simulate an older catalog version
    ds.deleteCategory(userId, "exp_groceries")
    assertTrue(ds.observeCategories(userId).first().none { it.id == "exp_groceries" })

    val addedCount = ds.syncMissingDefaultCategories(userId).getOrThrow()
    assertTrue(addedCount >= 1)

    val afterSync = ds.observeCategories(userId).first()
    assertTrue(afterSync.any { it.id == "exp_groceries" })
    val preservedFood = afterSync.first { it.id == "exp_food" }
    assertEquals("হোটেল ও খাবার", preservedFood.nameBn)
    assertEquals("fastfood", preservedFood.iconKey)
  }

  @Test
  fun `monthly archives preserve and aggregate all details per month`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ds = com.example.data.repository.LocalFinanceDataSource(context)
    val userId = "monthly_archive_test_user"
    ds.seedInitialDataIfNeeded(userId)
    ds.clearDemoData(userId)

    val oct15 = com.example.util.FinancialCalculator.defaultTimestampForMonth("2026-10")
    val nov10 = com.example.util.FinancialCalculator.defaultTimestampForMonth("2026-11")

    ds.upsertTransaction(
      userId,
      com.example.data.model.FinanceTransaction(
        type = "expense",
        amount = 1500.0,
        categoryId = "exp_groceries",
        categoryNameBn = "কাঁচাবাজার",
        categoryNameEn = "Groceries",
        timestampMillis = oct15,
        note = "অক্টোবর বাজার",
        paymentMethod = "mobile_banking"
      )
    )
    ds.upsertTransaction(
      userId,
      com.example.data.model.FinanceTransaction(
        type = "income",
        amount = 25000.0,
        categoryId = "inc_salary",
        categoryNameBn = "বেতন",
        categoryNameEn = "Salary",
        timestampMillis = oct15,
        note = "অক্টোবর বেতন",
        paymentMethod = "bank"
      )
    )

    val txs = ds.observeTransactions(userId).first()
    val cats = ds.observeCategories(userId).first()
    val budgets = ds.observeBudgets(userId).first()

    val archives = com.example.util.FinancialCalculator.computeMonthlyArchives(txs, cats, budgets, nov10)
    val octSummary = archives.firstOrNull { it.monthKey == "2026-10" }
    assertTrue(octSummary != null)
    assertEquals(25000.0, octSummary!!.totalIncome, 0.01)
    assertEquals(1500.0, octSummary.totalExpense, 0.01)
    assertEquals(23500.0, octSummary.netBalance, 0.01)
    assertEquals(2, octSummary.transactionCount)
    assertTrue(octSummary.topExpenseCategories.any { it.categoryId == "exp_groceries" && it.amount == 1500.0 })
    assertTrue(octSummary.paymentBreakdown.any { it.method == "mobile_banking" && it.amount == 1500.0 })
    assertEquals("2026-09", com.example.util.FinancialCalculator.adjacentMonthKey("2026-10", -1))
    assertEquals("2026-11", com.example.util.FinancialCalculator.adjacentMonthKey("2026-10", 1))
  }
}
