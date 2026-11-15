package com.example.data.repository

import android.content.Context
import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCatalog
import com.example.data.model.FinanceTransaction
import com.example.data.model.LendBorrowPayment
import com.example.data.model.LendBorrowRecord
import com.example.data.model.SavingsEntry
import com.example.data.model.SavingsGoal
import com.example.data.model.UserPreferences
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

class LocalFinanceDataSource(context: Context) : FinanceDataSource {

    private val storageFile = File(context.applicationContext.filesDir, "hisabkori_local_data.json")
    private val mutex = Mutex()

    private data class Snapshot(
        val preferences: UserPreferences = UserPreferences(userId = LOCAL_USER_ID),
        val categories: List<CategoryItem> = emptyList(),
        val transactions: List<FinanceTransaction> = emptyList(),
        val budgets: List<BudgetItem> = emptyList(),
        val savingsGoals: List<SavingsGoal> = emptyList(),
        val savingsEntries: List<SavingsEntry> = emptyList(),
        val lendBorrows: List<LendBorrowRecord> = emptyList(),
        val lendBorrowPayments: List<LendBorrowPayment> = emptyList()
    )

    private val state = MutableStateFlow(Snapshot())
    @Volatile private var loadedFromDisk = false

    override fun observePreferences(userId: String): Flow<UserPreferences> = state.map { it.preferences }
    override fun observeCategories(userId: String): Flow<List<CategoryItem>> =
        state.map { s -> s.categories.sortedWith(compareBy<CategoryItem> { it.type }.thenBy { it.nameEn }) }

    override fun observeTransactions(userId: String): Flow<List<FinanceTransaction>> =
        state.map { s -> s.transactions.sortedByDescending { it.timestampMillis } }

    override fun observeBudgets(userId: String): Flow<List<BudgetItem>> = state.map { it.budgets }
    override fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> = state.map { it.savingsGoals }
    override fun observeSavingsEntries(userId: String): Flow<List<SavingsEntry>> =
        state.map { s -> s.savingsEntries.sortedByDescending { it.timestampMillis } }

    override fun observeLendBorrows(userId: String): Flow<List<LendBorrowRecord>> =
        state.map { s -> s.lendBorrows.sortedByDescending { it.dateMillis } }

    override fun observeLendBorrowPayments(userId: String): Flow<List<LendBorrowPayment>> =
        state.map { s -> s.lendBorrowPayments.sortedByDescending { it.timestampMillis } }

    override suspend fun savePreferences(userId: String, prefs: UserPreferences): Result<Unit> =
        updateSnapshot { it.copy(preferences = prefs.copy(userId = LOCAL_USER_ID)) }.map { }

    override suspend fun upsertTransaction(
        userId: String,
        transaction: FinanceTransaction,
        documentId: String?
    ): Result<String> {
        val docId = documentId?.takeIf { it.isNotBlank() }
            ?: transaction.id.takeIf { it.isNotBlank() }
            ?: transaction.clientRequestId?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val item = transaction.copy(
            id = docId,
            userId = LOCAL_USER_ID,
            note = transaction.note.take(300),
            createdAt = transaction.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        return updateSnapshot { snap ->
            val list = snap.transactions.filterNot { it.id == docId } + item
            snap.copy(transactions = list)
        }.map { docId }
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> =
        updateSnapshot { snap ->
            snap.copy(transactions = snap.transactions.filterNot { it.id == transactionId })
        }.map { }

    override suspend fun upsertCategory(userId: String, category: CategoryItem): Result<String> {
        val docId = category.id.takeIf { it.isNotBlank() } ?: "cat_${UUID.randomUUID().toString().take(12)}"
        val item = category.copy(
            id = docId,
            userId = LOCAL_USER_ID,
            nameBn = category.nameBn.trim().take(60),
            nameEn = category.nameEn.trim().take(60),
            createdAt = category.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        return updateSnapshot { snap ->
            val list = snap.categories.filterNot { it.id == docId } + item
            snap.copy(categories = list)
        }.map { docId }
    }

    override suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> =
        updateSnapshot { snap ->
            snap.copy(categories = snap.categories.filterNot { it.id == categoryId })
        }.map { }

    override suspend fun upsertBudget(userId: String, budget: BudgetItem): Result<String> {
        val docId = budget.id.takeIf { it.isNotBlank() }
            ?: "bud_${budget.monthKey}_${budget.categoryId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val item = budget.copy(
            id = docId,
            userId = LOCAL_USER_ID,
            createdAt = budget.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        return updateSnapshot { snap ->
            val list = snap.budgets.filterNot { it.id == docId } + item
            snap.copy(budgets = list)
        }.map { docId }
    }

    override suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> =
        updateSnapshot { snap ->
            snap.copy(budgets = snap.budgets.filterNot { it.id == budgetId })
        }.map { }

    override suspend fun upsertSavingsGoal(userId: String, goal: SavingsGoal): Result<String> {
        val docId = goal.id.takeIf { it.isNotBlank() } ?: "goal_${UUID.randomUUID().toString().take(12)}"
        val item = goal.copy(
            id = docId,
            userId = LOCAL_USER_ID,
            title = goal.title.trim().take(80),
            createdAt = goal.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        return updateSnapshot { snap ->
            val list = snap.savingsGoals.filterNot { it.id == docId } + item
            snap.copy(savingsGoals = list)
        }.map { docId }
    }

    override suspend fun addSavingsContribution(
        userId: String,
        goal: SavingsGoal,
        amount: Double,
        note: String,
        timestampMillis: Long
    ): Result<Unit> {
        val entryId = "se_${UUID.randomUUID().toString().take(12)}"
        return updateSnapshot { snap ->
            val updatedGoals = snap.savingsGoals.map {
                if (it.id == goal.id) it.copy(savedAmount = it.savedAmount + amount, updatedAt = Timestamp.now()) else it
            }
            val newEntry = SavingsEntry(
                id = entryId,
                userId = LOCAL_USER_ID,
                goalId = goal.id,
                goalTitle = goal.title.take(80),
                amount = amount,
                note = note.trim().take(200),
                timestampMillis = timestampMillis,
                isDemo = false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            snap.copy(savingsGoals = updatedGoals, savingsEntries = snap.savingsEntries + newEntry)
        }.map { }
    }

    override suspend fun deleteSavingsGoal(userId: String, goalId: String): Result<Unit> =
        updateSnapshot { snap ->
            snap.copy(
                savingsGoals = snap.savingsGoals.filterNot { it.id == goalId },
                savingsEntries = snap.savingsEntries.filterNot { it.goalId == goalId }
            )
        }.map { }

    override suspend fun upsertLendBorrow(userId: String, record: LendBorrowRecord): Result<String> {
        val docId = record.id.takeIf { it.isNotBlank() } ?: "lb_${UUID.randomUUID().toString().take(12)}"
        val paid = record.paidAmount.coerceAtMost(record.totalAmount)
        val status = when {
            paid >= record.totalAmount -> "paid"
            paid > 0.0 -> "partial"
            else -> "unpaid"
        }
        val item = record.copy(
            id = docId,
            userId = LOCAL_USER_ID,
            personName = record.personName.trim().take(80),
            phoneNumber = record.phoneNumber.trim().take(30),
            paidAmount = paid,
            description = record.description.trim().take(300),
            status = status,
            createdAt = record.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        return updateSnapshot { snap ->
            val list = snap.lendBorrows.filterNot { it.id == docId } + item
            snap.copy(lendBorrows = list)
        }.map { docId }
    }

    override suspend fun addLendBorrowPayment(
        userId: String,
        record: LendBorrowRecord,
        paymentAmount: Double,
        note: String,
        timestampMillis: Long
    ): Result<Unit> {
        val paymentId = "lbp_${UUID.randomUUID().toString().take(12)}"
        val newPaid = (record.paidAmount + paymentAmount).coerceAtMost(record.totalAmount)
        val newStatus = if (newPaid >= record.totalAmount) "paid" else "partial"
        return updateSnapshot { snap ->
            val updatedRecords = snap.lendBorrows.map {
                if (it.id == record.id) it.copy(paidAmount = newPaid, status = newStatus, updatedAt = Timestamp.now()) else it
            }
            val newPayment = LendBorrowPayment(
                id = paymentId,
                userId = LOCAL_USER_ID,
                recordId = record.id,
                amount = paymentAmount,
                note = note.trim().take(200),
                timestampMillis = timestampMillis,
                isDemo = false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            snap.copy(
                lendBorrows = updatedRecords,
                lendBorrowPayments = snap.lendBorrowPayments + newPayment
            )
        }.map { }
    }

    override suspend fun deleteLendBorrow(userId: String, recordId: String): Result<Unit> =
        updateSnapshot { snap ->
            snap.copy(
                lendBorrows = snap.lendBorrows.filterNot { it.id == recordId },
                lendBorrowPayments = snap.lendBorrowPayments.filterNot { it.recordId == recordId }
            )
        }.map { }

    override suspend fun seedInitialDataIfNeeded(userId: String): Result<Unit> {
        return try {
            val resultSnap = mutex.withLock {
                var current = state.value
                if (!loadedFromDisk) {
                    current = withContext(Dispatchers.IO) { loadFromDiskSync() }
                    loadedFromDisk = true
                }
                val next = when {
                    current.preferences.demoSeeded && current.categories.isNotEmpty() -> current
                    current.categories.isNotEmpty() -> {
                        val updated = current.copy(preferences = current.preferences.copy(demoSeeded = true))
                        withContext(Dispatchers.IO) { writeToDiskSync(updated) }
                        updated
                    }
                    else -> {
                        val seeded = buildInitialDemoSnapshot()
                        withContext(Dispatchers.IO) { writeToDiskSync(seeded) }
                        seeded
                    }
                }
                state.value = next
                next
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncMissingDefaultCategories(userId: String): Result<Int> {
        return try {
            var addedCount = 0
            updateSnapshot { snap ->
                val existingIds = snap.categories.map { it.id }.toSet()
                val missing = DefaultCatalog.allDefaultCategories
                    .filter { it.id !in existingIds }
                    .map { seed ->
                        CategoryItem(
                            id = seed.id,
                            userId = LOCAL_USER_ID,
                            nameBn = seed.nameBn,
                            nameEn = seed.nameEn,
                            type = seed.type,
                            iconKey = seed.iconKey,
                            colorHex = seed.colorHex,
                            isDefault = true,
                            createdAt = Timestamp.now(),
                            updatedAt = Timestamp.now()
                        )
                    }
                addedCount = missing.size
                if (missing.isEmpty()) snap else snap.copy(categories = snap.categories + missing)
            }
            Result.success(addedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearDemoData(userId: String): Result<Unit> =
        updateSnapshot { snap ->
            snap.copy(
                preferences = snap.preferences.copy(demoSeeded = true),
                transactions = snap.transactions.filterNot { it.isDemo },
                budgets = snap.budgets.filterNot { it.isDemo },
                savingsGoals = snap.savingsGoals.filterNot { it.isDemo },
                savingsEntries = snap.savingsEntries.filterNot { it.isDemo },
                lendBorrows = snap.lendBorrows.filterNot { it.isDemo },
                lendBorrowPayments = snap.lendBorrowPayments.filterNot { it.isDemo }
            )
        }.map { }

    override suspend fun exportBackupJson(userId: String): Result<String> = runCatching {
        snapshotToJson(state.value, onlyNonDemo = false).toString(2)
    }

    fun exportNonDemoBackupJsonIfAny(): String? {
        val s = state.value
        val hasCustomData = s.transactions.any { !it.isDemo } ||
            s.budgets.any { !it.isDemo } ||
            s.savingsGoals.any { !it.isDemo } ||
            s.lendBorrows.any { !it.isDemo } ||
            s.categories.any { !it.isDefault }
        if (!hasCustomData) return null
        return snapshotToJson(s, onlyNonDemo = true).toString()
    }

    override suspend fun restoreBackupJson(userId: String, jsonText: String): Result<Int> {
        return try {
            val parsed = parseBackupJson(jsonText)
            var count = 0
            updateSnapshot { snap ->
                val catMap = snap.categories.associateBy { it.id }.toMutableMap()
                parsed.categories.forEach { catMap[it.id] = it; count++ }
                val txMap = snap.transactions.associateBy { it.id }.toMutableMap()
                parsed.transactions.forEach { txMap[it.id] = it; count++ }
                val budMap = snap.budgets.associateBy { it.id }.toMutableMap()
                parsed.budgets.forEach { budMap[it.id] = it; count++ }
                val goalMap = snap.savingsGoals.associateBy { it.id }.toMutableMap()
                parsed.savingsGoals.forEach { goalMap[it.id] = it; count++ }
                val seMap = snap.savingsEntries.associateBy { it.id }.toMutableMap()
                parsed.savingsEntries.forEach { seMap[it.id] = it; count++ }
                val lbMap = snap.lendBorrows.associateBy { it.id }.toMutableMap()
                parsed.lendBorrows.forEach { lbMap[it.id] = it; count++ }
                val lbpMap = snap.lendBorrowPayments.associateBy { it.id }.toMutableMap()
                parsed.lendBorrowPayments.forEach { lbpMap[it.id] = it; count++ }

                snap.copy(
                    categories = catMap.values.toList(),
                    transactions = txMap.values.toList(),
                    budgets = budMap.values.toList(),
                    savingsGoals = goalMap.values.toList(),
                    savingsEntries = seMap.values.toList(),
                    lendBorrows = lbMap.values.toList(),
                    lendBorrowPayments = lbpMap.values.toList()
                )
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateSnapshot(transform: (Snapshot) -> Snapshot): Result<Snapshot> {
        return try {
            val next = mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                withContext(Dispatchers.IO) {
                    writeToDiskSync(updated)
                }
                updated
            }
            Result.success(next)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildInitialDemoSnapshot(): Snapshot {
        val now = System.currentTimeMillis()
        val monthKey = DefaultCatalog.currentMonthKey(now)
        fun daysAgo(days: Int, hour: Int = 11): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 0)
            }
            return cal.timeInMillis
        }

        fun lastMonthDay(dayOfMonth: Int): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                set(Calendar.HOUR_OF_DAY, 14)
                set(Calendar.MINUTE, 0)
            }
            return cal.timeInMillis
        }

        val prefs = UserPreferences(
            userId = LOCAL_USER_ID,
            language = "bn",
            themeMode = "light",
            textScale = "medium",
            currencyCode = "BDT",
            monthlySavingsGoal = 12000.0,
            demoSeeded = true
        )
        val cats = DefaultCatalog.allDefaultCategories.map { seed ->
            CategoryItem(
                id = seed.id,
                userId = LOCAL_USER_ID,
                nameBn = seed.nameBn,
                nameEn = seed.nameEn,
                type = seed.type,
                iconKey = seed.iconKey,
                colorHex = seed.colorHex,
                isDefault = true
            )
        }

        val txs = listOf(
            FinanceTransaction("demo_tx_1", LOCAL_USER_ID, "income", 52000.0, "inc_salary", "বেতন", "Salary", daysAgo(6, 10), "মাসিক বেতন (ডেমো)", "bank", isDemo = true),
            FinanceTransaction("demo_tx_2", LOCAL_USER_ID, "income", 8500.0, "inc_freelance", "ফ্রিল্যান্স", "Freelance", daysAgo(2, 16), "ডিজাইন প্রজেক্ট পেমেন্ট", "mobile_banking", isDemo = true),
            FinanceTransaction("demo_tx_3", LOCAL_USER_ID, "income", 2500.0, "inc_gift", "উপহার", "Gift", daysAgo(0, 9), "পরিবার থেকে উপহার", "cash", isDemo = true),
            FinanceTransaction("demo_tx_4", LOCAL_USER_ID, "expense", 15000.0, "exp_rent", "বাড়ি ভাড়া", "House Rent", daysAgo(5, 11), "মাসিক বাসা ভাড়া", "bank", isDemo = true),
            FinanceTransaction("demo_tx_5", LOCAL_USER_ID, "expense", 3450.0, "exp_food", "খাবার", "Food", daysAgo(4, 18), "সাপ্তাহিক কাঁচাবাজার ও চাল-ডাল", "cash", isDemo = true),
            FinanceTransaction("demo_tx_6", LOCAL_USER_ID, "expense", 2200.0, "exp_bills", "বিদ্যুৎ ও গ্যাস বিল", "Electricity & Gas Bills", daysAgo(3, 13), "ডেসকো প্রিপেইড মিটার ও গ্যাস", "mobile_banking", isDemo = true),
            FinanceTransaction("demo_tx_7", LOCAL_USER_ID, "expense", 1200.0, "exp_mobile", "মোবাইল ও ইন্টারনেট", "Mobile & Internet", daysAgo(2, 12), "ব্রডব্যান্ড ও মোবাইল ডাটা", "mobile_banking", isDemo = true),
            FinanceTransaction("demo_tx_8", LOCAL_USER_ID, "expense", 680.0, "exp_transport", "যাতায়াত", "Transport", daysAgo(1, 19), "মেট্রোরেল ও রিকশা ভাড়া", "cash", isDemo = true),
            FinanceTransaction("demo_tx_9", LOCAL_USER_ID, "expense", 3200.0, "exp_shopping", "শপিং", "Shopping", daysAgo(1, 17), "শীতের পোশাক কেনাকাটা", "mobile_banking", isDemo = true),
            FinanceTransaction("demo_tx_10", LOCAL_USER_ID, "expense", 540.0, "exp_food", "খাবার", "Food", daysAgo(0, 14), "দুপুরের খাবার ও চা", "cash", isDemo = true),
            FinanceTransaction("demo_tx_11", LOCAL_USER_ID, "expense", 220.0, "exp_transport", "যাতায়াত", "Transport", daysAgo(0, 10), "অফিস যাতায়াত", "cash", isDemo = true),
            FinanceTransaction("demo_tx_12", LOCAL_USER_ID, "income", 50000.0, "inc_salary", "বেতন", "Salary", lastMonthDay(5), "গত মাসের বেতন", "bank", isDemo = true),
            FinanceTransaction("demo_tx_13", LOCAL_USER_ID, "expense", 15000.0, "exp_rent", "বাড়ি ভাড়া", "House Rent", lastMonthDay(6), "গত মাসের বাসা ভাড়া", "bank", isDemo = true),
            FinanceTransaction("demo_tx_14", LOCAL_USER_ID, "expense", 8200.0, "exp_food", "খাবার", "Food", lastMonthDay(14), "গত মাসের বাজার খরচ", "cash", isDemo = true),
            FinanceTransaction("demo_tx_15", LOCAL_USER_ID, "expense", 2900.0, "exp_transport", "যাতায়াত", "Transport", lastMonthDay(20), "যাতায়াত ও যন্ত্রাংশ", "cash", isDemo = true)
        )

        val budgets = listOf(
            BudgetItem("demo_bud_${monthKey}_exp_rent", LOCAL_USER_ID, "exp_rent", "বাড়ি ভাড়া", "House Rent", monthKey, 15000.0, isDemo = true),
            BudgetItem("demo_bud_${monthKey}_exp_food", LOCAL_USER_ID, "exp_food", "খাবার", "Food", monthKey, 5000.0, isDemo = true),
            BudgetItem("demo_bud_${monthKey}_exp_shopping", LOCAL_USER_ID, "exp_shopping", "শপিং", "Shopping", monthKey, 3000.0, isDemo = true),
            BudgetItem("demo_bud_${monthKey}_exp_transport", LOCAL_USER_ID, "exp_transport", "যাতায়াত", "Transport", monthKey, 2500.0, isDemo = true),
            BudgetItem("demo_bud_${monthKey}_exp_bills", LOCAL_USER_ID, "exp_bills", "বিদ্যুৎ ও গ্যাস বিল", "Electricity & Gas Bills", monthKey, 3000.0, isDemo = true),
            BudgetItem("demo_bud_${monthKey}_exp_mobile", LOCAL_USER_ID, "exp_mobile", "মোবাইল ও ইন্টারনেট", "Mobile & Internet", monthKey, 1500.0, isDemo = true)
        )

        val goals = listOf(
            SavingsGoal("demo_goal_emergency", LOCAL_USER_ID, "জরুরি তহবিল", 50000.0, 18500.0, now + 120L * 86400000L, "savings", 0xFF16A34AL, isDemo = true),
            SavingsGoal("demo_goal_laptop", LOCAL_USER_ID, "নতুন ল্যাপটপ তহবিল", 75000.0, 25000.0, now + 180L * 86400000L, "laptop", 0xFF2563EBL, isDemo = true)
        )
        val entries = listOf(
            SavingsEntry("demo_se_1", LOCAL_USER_ID, "demo_goal_emergency", "জরুরি তহবিল", 8500.0, "মাসিক সঞ্চয় জমা", daysAgo(4, 15), isDemo = true)
        )
        val lbs = listOf(
            LendBorrowRecord("demo_lb_1", LOCAL_USER_ID, "lend", "করিম আহমেদ", "01711-223344", 5000.0, 2000.0, daysAgo(10, 12), now + 7L * 86400000L, "জরুরি প্রয়োজনে ধার", "partial", isDemo = true),
            LendBorrowRecord("demo_lb_2", LOCAL_USER_ID, "borrow", "তানভীর হাসান", "01819-556677", 3000.0, 0.0, daysAgo(5, 14), now + 10L * 86400000L, "বই ও কোর্স ফি বাবদ ধার", "unpaid", isDemo = true)
        )
        val lbps = listOf(
            LendBorrowPayment("demo_lbp_1", LOCAL_USER_ID, "demo_lb_1", 2000.0, "প্রথম কিস্তি পরিশোধ", daysAgo(3, 18), isDemo = true)
        )

        return Snapshot(
            preferences = prefs,
            categories = cats,
            transactions = txs,
            budgets = budgets,
            savingsGoals = goals,
            savingsEntries = entries,
            lendBorrows = lbs,
            lendBorrowPayments = lbps
        )
    }

    private fun loadFromDiskSync(): Snapshot {
        if (!storageFile.exists()) return Snapshot()
        return try {
            val root = JSONObject(storageFile.readText())
            val pObj = root.optJSONObject("preferences")
            val prefs = if (pObj != null) {
                UserPreferences(
                    userId = LOCAL_USER_ID,
                    language = pObj.optString("language", "bn"),
                    themeMode = pObj.optString("themeMode", "light"),
                    textScale = pObj.optString("textScale", "medium"),
                    currencyCode = pObj.optString("currencyCode", "BDT"),
                    monthlySavingsGoal = pObj.optDouble("monthlySavingsGoal", 12000.0),
                    biometricLockEnabled = pObj.optBoolean("biometricLockEnabled", false),
                    pinEnabled = pObj.optBoolean("pinEnabled", false),
                    pinHash = pObj.optString("pinHash", "").ifBlank { null },
                    deletePasswordEnabled = pObj.optBoolean("deletePasswordEnabled", false),
                    deletePasswordHash = pObj.optString("deletePasswordHash", "").ifBlank { null },
                    hideBalances = pObj.optBoolean("hideBalances", false),
                    demoSeeded = pObj.optBoolean("demoSeeded", false)
                )
            } else {
                UserPreferences(userId = LOCAL_USER_ID)
            }
            val parsed = parseBackupJson(root.toString())
            val entries = mutableListOf<SavingsEntry>()
            root.optJSONArray("savingsEntries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    entries.add(
                        SavingsEntry(
                            id = o.optString("id"),
                            userId = LOCAL_USER_ID,
                            goalId = o.optString("goalId"),
                            goalTitle = o.optString("goalTitle"),
                            amount = o.optDouble("amount", 0.0),
                            note = o.optString("note"),
                            timestampMillis = o.optLong("timestampMillis", 0L),
                            isDemo = o.optBoolean("isDemo", false)
                        )
                    )
                }
            }
            val payments = mutableListOf<LendBorrowPayment>()
            root.optJSONArray("lendBorrowPayments")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    payments.add(
                        LendBorrowPayment(
                            id = o.optString("id"),
                            userId = LOCAL_USER_ID,
                            recordId = o.optString("recordId"),
                            amount = o.optDouble("amount", 0.0),
                            note = o.optString("note"),
                            timestampMillis = o.optLong("timestampMillis", 0L),
                            isDemo = o.optBoolean("isDemo", false)
                        )
                    )
                }
            }
            parsed.copy(
                preferences = prefs,
                savingsEntries = entries,
                lendBorrowPayments = payments
            )
        } catch (_: Exception) {
            Snapshot()
        }
    }

    private fun writeToDiskSync(snap: Snapshot) {
        val root = snapshotToJson(snap, onlyNonDemo = false)
        root.put("preferences", JSONObject().apply {
            put("language", snap.preferences.language)
            put("themeMode", snap.preferences.themeMode)
            put("textScale", snap.preferences.textScale)
            put("currencyCode", snap.preferences.currencyCode)
            put("monthlySavingsGoal", snap.preferences.monthlySavingsGoal)
            put("biometricLockEnabled", snap.preferences.biometricLockEnabled)
            put("pinEnabled", snap.preferences.pinEnabled)
            put("pinHash", snap.preferences.pinHash ?: "")
            put("deletePasswordEnabled", snap.preferences.deletePasswordEnabled)
            put("deletePasswordHash", snap.preferences.deletePasswordHash ?: "")
            put("hideBalances", snap.preferences.hideBalances)
            put("demoSeeded", snap.preferences.demoSeeded)
        })
        storageFile.writeText(root.toString())
    }

    private fun snapshotToJson(snap: Snapshot, onlyNonDemo: Boolean): JSONObject {
        val root = JSONObject()
        root.put("app", "HisabKori")
        root.put("exportedAtMillis", System.currentTimeMillis())

        val catsArr = JSONArray()
        snap.categories.filter { !onlyNonDemo || !it.isDefault }.forEach { c ->
            catsArr.put(JSONObject().apply {
                put("id", c.id)
                put("nameBn", c.nameBn)
                put("nameEn", c.nameEn)
                put("type", c.type)
                put("iconKey", c.iconKey)
                put("colorHex", c.colorHex)
                put("isDefault", c.isDefault)
            })
        }
        root.put("categories", catsArr)

        val txArr = JSONArray()
        snap.transactions.filter { !onlyNonDemo || !it.isDemo }.forEach { t ->
            txArr.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("amount", t.amount)
                put("categoryId", t.categoryId)
                put("categoryNameBn", t.categoryNameBn)
                put("categoryNameEn", t.categoryNameEn)
                put("timestampMillis", t.timestampMillis)
                put("note", t.note)
                put("paymentMethod", t.paymentMethod)
                put("photoUri", t.photoUri ?: "")
                put("isDemo", t.isDemo)
            })
        }
        root.put("transactions", txArr)

        val budArr = JSONArray()
        snap.budgets.filter { !onlyNonDemo || !it.isDemo }.forEach { b ->
            budArr.put(JSONObject().apply {
                put("id", b.id)
                put("categoryId", b.categoryId)
                put("categoryNameBn", b.categoryNameBn)
                put("categoryNameEn", b.categoryNameEn)
                put("monthKey", b.monthKey)
                put("limitAmount", b.limitAmount)
                put("isDemo", b.isDemo)
            })
        }
        root.put("budgets", budArr)

        val goalArr = JSONArray()
        snap.savingsGoals.filter { !onlyNonDemo || !it.isDemo }.forEach { g ->
            goalArr.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("targetAmount", g.targetAmount)
                put("savedAmount", g.savedAmount)
                put("targetDateMillis", g.targetDateMillis)
                put("iconKey", g.iconKey)
                put("colorHex", g.colorHex)
                put("isDemo", g.isDemo)
            })
        }
        root.put("savingsGoals", goalArr)

        val seArr = JSONArray()
        snap.savingsEntries.filter { !onlyNonDemo || !it.isDemo }.forEach { e ->
            seArr.put(JSONObject().apply {
                put("id", e.id)
                put("goalId", e.goalId)
                put("goalTitle", e.goalTitle)
                put("amount", e.amount)
                put("note", e.note)
                put("timestampMillis", e.timestampMillis)
                put("isDemo", e.isDemo)
            })
        }
        root.put("savingsEntries", seArr)

        val lbArr = JSONArray()
        snap.lendBorrows.filter { !onlyNonDemo || !it.isDemo }.forEach { r ->
            lbArr.put(JSONObject().apply {
                put("id", r.id)
                put("kind", r.kind)
                put("personName", r.personName)
                put("phoneNumber", r.phoneNumber)
                put("totalAmount", r.totalAmount)
                put("paidAmount", r.paidAmount)
                put("dateMillis", r.dateMillis)
                put("dueDateMillis", r.dueDateMillis)
                put("description", r.description)
                put("status", r.status)
                put("isDemo", r.isDemo)
            })
        }
        root.put("lendBorrows", lbArr)

        val lbpArr = JSONArray()
        snap.lendBorrowPayments.filter { !onlyNonDemo || !it.isDemo }.forEach { p ->
            lbpArr.put(JSONObject().apply {
                put("id", p.id)
                put("recordId", p.recordId)
                put("amount", p.amount)
                put("note", p.note)
                put("timestampMillis", p.timestampMillis)
                put("isDemo", p.isDemo)
            })
        }
        root.put("lendBorrowPayments", lbpArr)

        return root
    }

    private fun parseBackupJson(jsonText: String): Snapshot {
        val root = JSONObject(jsonText)
        val cats = mutableListOf<CategoryItem>()
        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id").ifBlank { "cat_${UUID.randomUUID().toString().take(10)}" }
                cats.add(
                    CategoryItem(
                        id = id,
                        userId = LOCAL_USER_ID,
                        nameBn = obj.optString("nameBn", "ক্যাটাগরি").take(60).ifBlank { "ক্যাটাগরি" },
                        nameEn = obj.optString("nameEn", "Category").take(60).ifBlank { "Category" },
                        type = if (obj.optString("type") == "income") "income" else "expense",
                        iconKey = obj.optString("iconKey", "category").take(40).ifBlank { "category" },
                        colorHex = obj.optLong("colorHex", 0xFFFF5722L),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
        }
        val txs = mutableListOf<FinanceTransaction>()
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val amount = obj.optDouble("amount", 0.0)
                if (amount <= 0.0) continue
                val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
                val method = obj.optString("paymentMethod", "cash").let {
                    if (it in listOf("cash", "bank", "mobile_banking", "other")) it else "cash"
                }
                txs.add(
                    FinanceTransaction(
                        id = id,
                        userId = LOCAL_USER_ID,
                        type = if (obj.optString("type") == "income") "income" else "expense",
                        amount = amount,
                        categoryId = obj.optString("categoryId", "exp_other").ifBlank { "exp_other" },
                        categoryNameBn = obj.optString("categoryNameBn", "অন্যান্য").ifBlank { "অন্যান্য" },
                        categoryNameEn = obj.optString("categoryNameEn", "Other").ifBlank { "Other" },
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                        note = obj.optString("note", "").take(300),
                        paymentMethod = method,
                        photoUri = obj.optString("photoUri", "").ifBlank { null },
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
        }
        val buds = mutableListOf<BudgetItem>()
        root.optJSONArray("budgets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val limit = obj.optDouble("limitAmount", 0.0)
                if (limit <= 0.0) continue
                val id = obj.optString("id").ifBlank { "bud_${UUID.randomUUID().toString().take(10)}" }
                buds.add(
                    BudgetItem(
                        id = id,
                        userId = LOCAL_USER_ID,
                        categoryId = obj.optString("categoryId", "exp_other").ifBlank { "exp_other" },
                        categoryNameBn = obj.optString("categoryNameBn", "অন্যান্য").ifBlank { "অন্যান্য" },
                        categoryNameEn = obj.optString("categoryNameEn", "Other").ifBlank { "Other" },
                        monthKey = obj.optString("monthKey", DefaultCatalog.currentMonthKey()),
                        limitAmount = limit,
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
        }
        val goals = mutableListOf<SavingsGoal>()
        root.optJSONArray("savingsGoals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val target = obj.optDouble("targetAmount", 0.0)
                if (target <= 0.0) continue
                val id = obj.optString("id").ifBlank { "goal_${UUID.randomUUID().toString().take(10)}" }
                goals.add(
                    SavingsGoal(
                        id = id,
                        userId = LOCAL_USER_ID,
                        title = obj.optString("title", "Savings Goal").take(80).ifBlank { "Savings Goal" },
                        targetAmount = target,
                        savedAmount = obj.optDouble("savedAmount", 0.0).coerceAtLeast(0.0),
                        targetDateMillis = obj.optLong("targetDateMillis", 0L).coerceAtLeast(0L),
                        iconKey = obj.optString("iconKey", "savings").ifBlank { "savings" },
                        colorHex = obj.optLong("colorHex", 0xFF16A34AL),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
        }
        val lbs = mutableListOf<LendBorrowRecord>()
        root.optJSONArray("lendBorrows")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val total = obj.optDouble("totalAmount", 0.0)
                if (total <= 0.0) continue
                val paid = obj.optDouble("paidAmount", 0.0).coerceIn(0.0, total)
                val status = when {
                    paid >= total -> "paid"
                    paid > 0.0 -> "partial"
                    else -> "unpaid"
                }
                val id = obj.optString("id").ifBlank { "lb_${UUID.randomUUID().toString().take(10)}" }
                lbs.add(
                    LendBorrowRecord(
                        id = id,
                        userId = LOCAL_USER_ID,
                        kind = if (obj.optString("kind") == "borrow") "borrow" else "lend",
                        personName = obj.optString("personName", "Person").take(80).ifBlank { "Person" },
                        phoneNumber = obj.optString("phoneNumber", "").take(30),
                        totalAmount = total,
                        paidAmount = paid,
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                        dueDateMillis = obj.optLong("dueDateMillis", 0L).coerceAtLeast(0L),
                        description = obj.optString("description", "").take(300),
                        status = status,
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
        }
        val seList = mutableListOf<SavingsEntry>()
        root.optJSONArray("savingsEntries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val amt = o.optDouble("amount", 0.0)
                if (amt <= 0.0) continue
                val id = o.optString("id").ifBlank { "se_${UUID.randomUUID().toString().take(10)}" }
                seList.add(
                    SavingsEntry(
                        id = id,
                        userId = LOCAL_USER_ID,
                        goalId = o.optString("goalId"),
                        goalTitle = o.optString("goalTitle", "Savings").take(80).ifBlank { "Savings" },
                        amount = amt,
                        note = o.optString("note", "").take(200),
                        timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                        isDemo = o.optBoolean("isDemo", false)
                    )
                )
            }
        }
        val lbpList = mutableListOf<LendBorrowPayment>()
        root.optJSONArray("lendBorrowPayments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val amt = o.optDouble("amount", 0.0)
                if (amt <= 0.0) continue
                val id = o.optString("id").ifBlank { "lbp_${UUID.randomUUID().toString().take(10)}" }
                lbpList.add(
                    LendBorrowPayment(
                        id = id,
                        userId = LOCAL_USER_ID,
                        recordId = o.optString("recordId"),
                        amount = amt,
                        note = o.optString("note", "").take(200),
                        timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                        isDemo = o.optBoolean("isDemo", false)
                    )
                )
            }
        }
        return Snapshot(
            categories = cats,
            transactions = txs,
            budgets = buds,
            savingsGoals = goals,
            savingsEntries = seList,
            lendBorrows = lbs,
            lendBorrowPayments = lbpList
        )
    }

    companion object {
        const val LOCAL_USER_ID = "local_guest"
    }
}
