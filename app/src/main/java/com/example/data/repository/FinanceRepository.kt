package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCatalog
import com.example.data.model.FinanceTransaction
import com.example.data.model.LendBorrowPayment
import com.example.data.model.LendBorrowRecord
import com.example.data.model.SavingsEntry
import com.example.data.model.SavingsGoal
import com.example.data.model.UserPreferences
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth? = null
) : FinanceDataSource {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = FirebaseAuth.getInstance()
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun prefsDoc(userId: String) = userDoc(userId).collection("settings").document("preferences")
    private fun categoriesCol(userId: String) = userDoc(userId).collection("categories")
    private fun transactionsCol(userId: String) = userDoc(userId).collection("transactions")
    private fun budgetsCol(userId: String) = userDoc(userId).collection("budgets")
    private fun savingsGoalsCol(userId: String) = userDoc(userId).collection("savingsGoals")
    private fun savingsEntriesCol(userId: String) = userDoc(userId).collection("savingsEntries")
    private fun lendBorrowsCol(userId: String) = userDoc(userId).collection("lendBorrows")
    private fun lendBorrowPaymentsCol(userId: String) = userDoc(userId).collection("lendBorrowPayments")

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
        throw error
    }

    override fun observePreferences(userId: String): Flow<UserPreferences> =
        prefsDoc(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserPreferences::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserPreferences(userId = userId)
                } else {
                    UserPreferences(userId = userId)
                }
            }
            .logFirestoreErrors(OperationType.GET, prefsDoc(userId).path)

    override fun observeCategories(userId: String): Flow<List<CategoryItem>> =
        categoriesCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CategoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<CategoryItem> { it.type }.thenBy { it.nameEn })
            }
            .logFirestoreErrors(OperationType.LIST, categoriesCol(userId).path)

    override fun observeTransactions(userId: String): Flow<List<FinanceTransaction>> =
        transactionsCol(userId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, transactionsCol(userId).path)

    override fun observeBudgets(userId: String): Flow<List<BudgetItem>> =
        budgetsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BudgetItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, budgetsCol(userId).path)

    override fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> =
        savingsGoalsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, savingsGoalsCol(userId).path)

    override fun observeSavingsEntries(userId: String): Flow<List<SavingsEntry>> =
        savingsEntriesCol(userId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavingsEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, savingsEntriesCol(userId).path)

    override fun observeLendBorrows(userId: String): Flow<List<LendBorrowRecord>> =
        lendBorrowsCol(userId)
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LendBorrowRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, lendBorrowsCol(userId).path)

    override fun observeLendBorrowPayments(userId: String): Flow<List<LendBorrowPayment>> =
        lendBorrowPaymentsCol(userId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LendBorrowPayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, lendBorrowPaymentsCol(userId).path)

    override suspend fun savePreferences(userId: String, prefs: UserPreferences): Result<Unit> {
        val docRef = prefsDoc(userId)
        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "language" to prefs.language,
                    "themeMode" to prefs.themeMode,
                    "textScale" to prefs.textScale,
                    "currencyCode" to prefs.currencyCode,
                    "monthlySavingsGoal" to prefs.monthlySavingsGoal,
                    "biometricLockEnabled" to prefs.biometricLockEnabled,
                    "pinEnabled" to prefs.pinEnabled,
                    "deletePasswordEnabled" to prefs.deletePasswordEnabled,
                    "hideBalances" to prefs.hideBalances,
                    "demoSeeded" to prefs.demoSeeded,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!prefs.pinHash.isNullOrBlank()) {
                    payload["pinHash"] = prefs.pinHash
                }
                if (!prefs.deletePasswordHash.isNullOrBlank()) {
                    payload["deletePasswordHash"] = prefs.deletePasswordHash
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any?>(
                    "language" to prefs.language,
                    "themeMode" to prefs.themeMode,
                    "textScale" to prefs.textScale,
                    "currencyCode" to prefs.currencyCode,
                    "monthlySavingsGoal" to prefs.monthlySavingsGoal,
                    "biometricLockEnabled" to prefs.biometricLockEnabled,
                    "pinEnabled" to prefs.pinEnabled,
                    "pinHash" to prefs.pinHash,
                    "deletePasswordEnabled" to prefs.deletePasswordEnabled,
                    "deletePasswordHash" to prefs.deletePasswordHash,
                    "hideBalances" to prefs.hideBalances,
                    "demoSeeded" to prefs.demoSeeded,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun upsertTransaction(
        userId: String,
        transaction: FinanceTransaction,
        documentId: String?
    ): Result<String> {
        val docId = documentId?.takeIf { it.isNotBlank() }
            ?: transaction.id.takeIf { it.isNotBlank() }
            ?: transaction.clientRequestId?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val docRef = transactionsCol(userId).document(docId)
        return try {
            val isUpdate = transaction.id.isNotBlank() && transaction.createdAt != null
            if (isUpdate) {
                val updates = mutableMapOf<String, Any?>(
                    "type" to transaction.type,
                    "amount" to transaction.amount,
                    "categoryId" to transaction.categoryId,
                    "categoryNameBn" to transaction.categoryNameBn,
                    "categoryNameEn" to transaction.categoryNameEn,
                    "timestampMillis" to transaction.timestampMillis,
                    "note" to transaction.note.take(300),
                    "paymentMethod" to transaction.paymentMethod,
                    "photoUri" to transaction.photoUri,
                    "isDemo" to transaction.isDemo,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "type" to transaction.type,
                    "amount" to transaction.amount,
                    "categoryId" to transaction.categoryId,
                    "categoryNameBn" to transaction.categoryNameBn,
                    "categoryNameEn" to transaction.categoryNameEn,
                    "timestampMillis" to transaction.timestampMillis,
                    "note" to transaction.note.take(300),
                    "paymentMethod" to transaction.paymentMethod,
                    "isDemo" to transaction.isDemo,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.photoUri?.takeIf { it.isNotBlank() }?.let { payload["photoUri"] = it }
                transaction.clientRequestId?.takeIf { it.isNotBlank() }?.let { payload["clientRequestId"] = it }
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docRef = transactionsCol(userId).document(transactionId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun upsertCategory(userId: String, category: CategoryItem): Result<String> {
        val docId = category.id.takeIf { it.isNotBlank() } ?: "cat_${UUID.randomUUID().toString().take(12)}"
        val docRef = categoriesCol(userId).document(docId)
        return try {
            if (category.id.isNotBlank() && category.createdAt != null) {
                val updates = mapOf(
                    "nameBn" to category.nameBn.trim().take(60),
                    "nameEn" to category.nameEn.trim().take(60),
                    "type" to category.type,
                    "iconKey" to category.iconKey,
                    "colorHex" to category.colorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "nameBn" to category.nameBn.trim().take(60),
                    "nameEn" to category.nameEn.trim().take(60),
                    "type" to category.type,
                    "iconKey" to category.iconKey,
                    "colorHex" to category.colorHex,
                    "isDefault" to category.isDefault,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> {
        val docRef = categoriesCol(userId).document(categoryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun upsertBudget(userId: String, budget: BudgetItem): Result<String> {
        val docId = budget.id.takeIf { it.isNotBlank() }
            ?: "bud_${budget.monthKey}_${budget.categoryId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docRef = budgetsCol(userId).document(docId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "categoryId" to budget.categoryId,
                        "categoryNameBn" to budget.categoryNameBn,
                        "categoryNameEn" to budget.categoryNameEn,
                        "monthKey" to budget.monthKey,
                        "limitAmount" to budget.limitAmount,
                        "isDemo" to budget.isDemo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "categoryId" to budget.categoryId,
                        "categoryNameBn" to budget.categoryNameBn,
                        "categoryNameEn" to budget.categoryNameEn,
                        "monthKey" to budget.monthKey,
                        "limitAmount" to budget.limitAmount,
                        "isDemo" to budget.isDemo,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val docRef = budgetsCol(userId).document(budgetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun upsertSavingsGoal(userId: String, goal: SavingsGoal): Result<String> {
        val docId = goal.id.takeIf { it.isNotBlank() } ?: "goal_${UUID.randomUUID().toString().take(12)}"
        val docRef = savingsGoalsCol(userId).document(docId)
        return try {
            if (goal.id.isNotBlank() && goal.createdAt != null) {
                docRef.update(
                    mapOf(
                        "title" to goal.title.trim().take(80),
                        "targetAmount" to goal.targetAmount,
                        "savedAmount" to goal.savedAmount,
                        "targetDateMillis" to goal.targetDateMillis,
                        "iconKey" to goal.iconKey,
                        "colorHex" to goal.colorHex,
                        "isDemo" to goal.isDemo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "title" to goal.title.trim().take(80),
                        "targetAmount" to goal.targetAmount,
                        "savedAmount" to goal.savedAmount,
                        "targetDateMillis" to goal.targetDateMillis,
                        "iconKey" to goal.iconKey,
                        "colorHex" to goal.colorHex,
                        "isDemo" to goal.isDemo,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun addSavingsContribution(
        userId: String,
        goal: SavingsGoal,
        amount: Double,
        note: String,
        timestampMillis: Long
    ): Result<Unit> {
        val goalRef = savingsGoalsCol(userId).document(goal.id)
        val entryRef = savingsEntriesCol(userId).document("se_${UUID.randomUUID().toString().take(12)}")
        return try {
            val batch = db.batch()
            val newSaved = goal.savedAmount + amount
            batch.update(
                goalRef,
                mapOf(
                    "savedAmount" to newSaved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                entryRef,
                mapOf(
                    "userId" to userId,
                    "goalId" to goal.id,
                    "goalTitle" to goal.title.take(80),
                    "amount" to amount,
                    "note" to note.trim().take(200),
                    "timestampMillis" to timestampMillis,
                    "isDemo" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, entryRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteSavingsGoal(userId: String, goalId: String): Result<Unit> {
        val docRef = savingsGoalsCol(userId).document(goalId)
        return try {
            val entries = savingsEntriesCol(userId).whereEqualTo("goalId", goalId).get().await()
            val batch = db.batch()
            entries.documents.forEach { batch.delete(it.reference) }
            batch.delete(docRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun upsertLendBorrow(userId: String, record: LendBorrowRecord): Result<String> {
        val docId = record.id.takeIf { it.isNotBlank() } ?: "lb_${UUID.randomUUID().toString().take(12)}"
        val docRef = lendBorrowsCol(userId).document(docId)
        val computedStatus = when {
            record.paidAmount >= record.totalAmount -> "paid"
            record.paidAmount > 0.0 -> "partial"
            else -> "unpaid"
        }
        return try {
            if (record.id.isNotBlank() && record.createdAt != null) {
                docRef.update(
                    mapOf(
                        "kind" to record.kind,
                        "personName" to record.personName.trim().take(80),
                        "phoneNumber" to record.phoneNumber.trim().take(30),
                        "totalAmount" to record.totalAmount,
                        "paidAmount" to record.paidAmount.coerceAtMost(record.totalAmount),
                        "dateMillis" to record.dateMillis,
                        "dueDateMillis" to record.dueDateMillis,
                        "description" to record.description.trim().take(300),
                        "status" to computedStatus,
                        "isDemo" to record.isDemo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to userId,
                        "kind" to record.kind,
                        "personName" to record.personName.trim().take(80),
                        "phoneNumber" to record.phoneNumber.trim().take(30),
                        "totalAmount" to record.totalAmount,
                        "paidAmount" to record.paidAmount.coerceAtMost(record.totalAmount),
                        "dateMillis" to record.dateMillis,
                        "dueDateMillis" to record.dueDateMillis,
                        "description" to record.description.trim().take(300),
                        "status" to computedStatus,
                        "isDemo" to record.isDemo,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun addLendBorrowPayment(
        userId: String,
        record: LendBorrowRecord,
        paymentAmount: Double,
        note: String,
        timestampMillis: Long
    ): Result<Unit> {
        val recordRef = lendBorrowsCol(userId).document(record.id)
        val paymentRef = lendBorrowPaymentsCol(userId).document("lbp_${UUID.randomUUID().toString().take(12)}")
        val newPaid = (record.paidAmount + paymentAmount).coerceAtMost(record.totalAmount)
        val newStatus = if (newPaid >= record.totalAmount) "paid" else "partial"
        return try {
            val batch = db.batch()
            batch.update(
                recordRef,
                mapOf(
                    "paidAmount" to newPaid,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                paymentRef,
                mapOf(
                    "userId" to userId,
                    "recordId" to record.id,
                    "amount" to paymentAmount,
                    "note" to note.trim().take(200),
                    "timestampMillis" to timestampMillis,
                    "isDemo" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, paymentRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun deleteLendBorrow(userId: String, recordId: String): Result<Unit> {
        val docRef = lendBorrowsCol(userId).document(recordId)
        return try {
            val payments = lendBorrowPaymentsCol(userId).whereEqualTo("recordId", recordId).get().await()
            val batch = db.batch()
            payments.documents.forEach { batch.delete(it.reference) }
            batch.delete(docRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    override suspend fun seedInitialDataIfNeeded(userId: String): Result<Unit> {
        return try {
            val prefSnap = prefsDoc(userId).get().await()
            val existingCats = categoriesCol(userId).limit(1).get().await()
            if (prefSnap.exists() && prefSnap.getBoolean("demoSeeded") == true && !existingCats.isEmpty) {
                return Result.success(Unit)
            }
            if (!existingCats.isEmpty) {
                // Ensure preferences exist even if categories already existed
                if (!prefSnap.exists()) {
                    savePreferences(userId, UserPreferences(userId = userId, demoSeeded = true))
                }
                return Result.success(Unit)
            }

            val batch = db.batch()
            val now = System.currentTimeMillis()
            val monthKey = DefaultCatalog.currentMonthKey(now)

            // 1. Preferences (default Bengali + BDT + light Cream theme)
            batch.set(
                prefsDoc(userId),
                mapOf(
                    "userId" to userId,
                    "language" to "bn",
                    "themeMode" to "light",
                    "textScale" to "medium",
                    "currencyCode" to "BDT",
                    "monthlySavingsGoal" to 12000.0,
                    "biometricLockEnabled" to false,
                    "pinEnabled" to false,
                    "deletePasswordEnabled" to false,
                    "hideBalances" to false,
                    "demoSeeded" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 2. Default Categories
            DefaultCatalog.allDefaultCategories.forEach { cat ->
                batch.set(
                    categoriesCol(userId).document(cat.id),
                    mapOf(
                        "userId" to userId,
                        "nameBn" to cat.nameBn,
                        "nameEn" to cat.nameEn,
                        "type" to cat.type,
                        "iconKey" to cat.iconKey,
                        "colorHex" to cat.colorHex,
                        "isDefault" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 3. Realistic Demo Transactions (current month & previous month)
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

            data class DemoTx(
                val id: String,
                val type: String,
                val amount: Double,
                val catId: String,
                val catBn: String,
                val catEn: String,
                val ts: Long,
                val note: String,
                val method: String
            )

            val demoTxs = listOf(
                DemoTx("demo_tx_1", "income", 52000.0, "inc_salary", "বেতন", "Salary", daysAgo(6, 10), "মাসিক বেতন (ডেমো)", "bank"),
                DemoTx("demo_tx_2", "income", 8500.0, "inc_freelance", "ফ্রিল্যান্স", "Freelance", daysAgo(2, 16), "ডিজাইন প্রজেক্ট পেমেন্ট", "mobile_banking"),
                DemoTx("demo_tx_3", "income", 2500.0, "inc_gift", "উপহার", "Gift", daysAgo(0, 9), "পরিবার থেকে উপহার", "cash"),
                DemoTx("demo_tx_4", "expense", 15000.0, "exp_rent", "বাড়ি ভাড়া", "House Rent", daysAgo(5, 11), "নভেম্বর বাসা ভাড়া", "bank"),
                DemoTx("demo_tx_5", "expense", 3450.0, "exp_food", "খাবার", "Food", daysAgo(4, 18), "সাপ্তাহিক কাঁচাবাজার ও চাল-ডাল", "cash"),
                DemoTx("demo_tx_6", "expense", 2200.0, "exp_bills", "বিদ্যুৎ ও গ্যাস বিল", "Electricity & Gas Bills", daysAgo(3, 13), "ডেসকো প্রিপেইড মিটার ও গ্যাস", "mobile_banking"),
                DemoTx("demo_tx_7", "expense", 1200.0, "exp_mobile", "মোবাইল ও ইন্টারনেট", "Mobile & Internet", daysAgo(2, 12), "ব্রডব্যান্ড ও মোবাইল ডাটা", "mobile_banking"),
                DemoTx("demo_tx_8", "expense", 680.0, "exp_transport", "যাতায়াত", "Transport", daysAgo(1, 19), "মেট্রোরেল ও রিকশা ভাড়া", "cash"),
                DemoTx("demo_tx_9", "expense", 3200.0, "exp_shopping", "শপিং", "Shopping", daysAgo(1, 17), "শীতের পোশাক কেনাকাটা", "mobile_banking"),
                DemoTx("demo_tx_10", "expense", 540.0, "exp_food", "খাবার", "Food", daysAgo(0, 14), "দুপুরের খাবার ও চা", "cash"),
                DemoTx("demo_tx_11", "expense", 220.0, "exp_transport", "যাতায়াত", "Transport", daysAgo(0, 10), "অফিস যাতায়াত", "cash"),
                // Last month entries for comparison insights
                DemoTx("demo_tx_12", "income", 50000.0, "inc_salary", "বেতন", "Salary", lastMonthDay(5), "গত মাসের বেতন", "bank"),
                DemoTx("demo_tx_13", "expense", 15000.0, "exp_rent", "বাড়ি ভাড়া", "House Rent", lastMonthDay(6), "গত মাসের বাসা ভাড়া", "bank"),
                DemoTx("demo_tx_14", "expense", 8200.0, "exp_food", "খাবার", "Food", lastMonthDay(14), "গত মাসের বাজার খরচ", "cash"),
                DemoTx("demo_tx_15", "expense", 2900.0, "exp_transport", "যাতায়াত", "Transport", lastMonthDay(20), "যাতায়াত ও যন্ত্রাংশ", "cash")
            )

            demoTxs.forEach { tx ->
                batch.set(
                    transactionsCol(userId).document(tx.id),
                    mapOf(
                        "userId" to userId,
                        "type" to tx.type,
                        "amount" to tx.amount,
                        "categoryId" to tx.catId,
                        "categoryNameBn" to tx.catBn,
                        "categoryNameEn" to tx.catEn,
                        "timestampMillis" to tx.ts,
                        "note" to tx.note,
                        "paymentMethod" to tx.method,
                        "isDemo" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 4. Demo Budgets for current month (one >80%, one >100% to demonstrate alerts)
            val demoBudgets = listOf(
                Triple("exp_rent", "বাড়ি ভাড়া" to "House Rent", 15000.0),
                Triple("exp_food", "খাবার" to "Food", 5000.0),       // ~80% reached!
                Triple("exp_shopping", "শপিং" to "Shopping", 3000.0), // >100% exceeded!
                Triple("exp_transport", "যাতায়াত" to "Transport", 2500.0),
                Triple("exp_bills", "বিদ্যুৎ ও গ্যাস বিল" to "Electricity & Gas Bills", 3000.0),
                Triple("exp_mobile", "মোবাইল ও ইন্টারনেট" to "Mobile & Internet", 1500.0)
            )
            demoBudgets.forEach { (catId, names, limit) ->
                batch.set(
                    budgetsCol(userId).document("demo_bud_${monthKey}_$catId"),
                    mapOf(
                        "userId" to userId,
                        "categoryId" to catId,
                        "categoryNameBn" to names.first,
                        "categoryNameEn" to names.second,
                        "monthKey" to monthKey,
                        "limitAmount" to limit,
                        "isDemo" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 5. Demo Savings Goals + Entries
            batch.set(
                savingsGoalsCol(userId).document("demo_goal_emergency"),
                mapOf(
                    "userId" to userId,
                    "title" to "জরুরি তহবিল",
                    "targetAmount" to 50000.0,
                    "savedAmount" to 18500.0,
                    "targetDateMillis" to now + 120L * 86400000L,
                    "iconKey" to "savings",
                    "colorHex" to 0xFF10B981L,
                    "isDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                savingsGoalsCol(userId).document("demo_goal_laptop"),
                mapOf(
                    "userId" to userId,
                    "title" to "নতুন ল্যাপটপ তহবিল",
                    "targetAmount" to 75000.0,
                    "savedAmount" to 25000.0,
                    "targetDateMillis" to now + 180L * 86400000L,
                    "iconKey" to "laptop",
                    "colorHex" to 0xFF3B82F6L,
                    "isDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                savingsEntriesCol(userId).document("demo_se_1"),
                mapOf(
                    "userId" to userId,
                    "goalId" to "demo_goal_emergency",
                    "goalTitle" to "জরুরি তহবিল",
                    "amount" to 8500.0,
                    "note" to "মাসিক সঞ্চয় জমা",
                    "timestampMillis" to daysAgo(4, 15),
                    "isDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 6. Demo Lend & Borrow Records + Payment
            batch.set(
                lendBorrowsCol(userId).document("demo_lb_1"),
                mapOf(
                    "userId" to userId,
                    "kind" to "lend",
                    "personName" to "করিম আহমেদ",
                    "phoneNumber" to "01711-223344",
                    "totalAmount" to 5000.0,
                    "paidAmount" to 2000.0,
                    "dateMillis" to daysAgo(10, 12),
                    "dueDateMillis" to now + 7L * 86400000L,
                    "description" to "জরুরি প্রয়োজনে ধার",
                    "status" to "partial",
                    "isDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                lendBorrowPaymentsCol(userId).document("demo_lbp_1"),
                mapOf(
                    "userId" to userId,
                    "recordId" to "demo_lb_1",
                    "amount" to 2000.0,
                    "note" to "প্রথম কিস্তি পরিশোধ",
                    "timestampMillis" to daysAgo(3, 18),
                    "isDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                lendBorrowsCol(userId).document("demo_lb_2"),
                mapOf(
                    "userId" to userId,
                    "kind" to "borrow",
                    "personName" to "তানভীর হাসান",
                    "phoneNumber" to "01819-556677",
                    "totalAmount" to 3000.0,
                    "paidAmount" to 0.0,
                    "dateMillis" to daysAgo(5, 14),
                    "dueDateMillis" to now + 10L * 86400000L,
                    "description" to "বই ও কোর্স ফি বাবদ ধার",
                    "status" to "unpaid",
                    "isDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc(userId).path, auth)
            Result.failure(e)
        }
    }

    override suspend fun syncMissingDefaultCategories(userId: String): Result<Int> {
        return try {
            val existingSnap = categoriesCol(userId).get().await()
            val existingIds = existingSnap.documents.map { it.id }.toSet()
            val missing = DefaultCatalog.allDefaultCategories.filter { it.id !in existingIds }
            if (missing.isEmpty()) return Result.success(0)

            missing.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { cat ->
                    batch.set(
                        categoriesCol(userId).document(cat.id),
                        mapOf(
                            "userId" to userId,
                            "nameBn" to cat.nameBn,
                            "nameEn" to cat.nameEn,
                            "type" to cat.type,
                            "iconKey" to cat.iconKey,
                            "colorHex" to cat.colorHex,
                            "isDefault" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(missing.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, categoriesCol(userId).path, auth)
            Result.failure(e)
        }
    }

    override suspend fun clearDemoData(userId: String): Result<Unit> {
        return try {
            val collections = listOf(
                transactionsCol(userId),
                budgetsCol(userId),
                savingsEntriesCol(userId),
                savingsGoalsCol(userId),
                lendBorrowPaymentsCol(userId),
                lendBorrowsCol(userId)
            )
            val docsToDelete = mutableListOf<com.google.firebase.firestore.DocumentReference>()
            for (col in collections) {
                val snap = col.whereEqualTo("isDemo", true).get().await()
                docsToDelete.addAll(snap.documents.map { it.reference })
            }
            // Commit in chunks of 400
            docsToDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            prefsDoc(userId).update(
                mapOf(
                    "demoSeeded" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDoc(userId).path, auth)
            Result.failure(e)
        }
    }

    override suspend fun exportBackupJson(userId: String): Result<String> {
        return try {
            val root = JSONObject()
            root.put("app", "HisabKori")
            root.put("exportedAtMillis", System.currentTimeMillis())

            val categoriesArr = JSONArray()
            categoriesCol(userId).get().await().documents.forEach { doc ->
                categoriesArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("nameBn", doc.getString("nameBn") ?: "")
                    put("nameEn", doc.getString("nameEn") ?: "")
                    put("type", doc.getString("type") ?: "expense")
                    put("iconKey", doc.getString("iconKey") ?: "category")
                    put("colorHex", doc.getLong("colorHex") ?: 0xFFFF6B2CL)
                    put("isDefault", doc.getBoolean("isDefault") ?: false)
                })
            }
            root.put("categories", categoriesArr)

            val txArr = JSONArray()
            transactionsCol(userId).get().await().documents.forEach { doc ->
                txArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("type", doc.getString("type") ?: "expense")
                    put("amount", doc.getDouble("amount") ?: 0.0)
                    put("categoryId", doc.getString("categoryId") ?: "")
                    put("categoryNameBn", doc.getString("categoryNameBn") ?: "")
                    put("categoryNameEn", doc.getString("categoryNameEn") ?: "")
                    put("timestampMillis", doc.getLong("timestampMillis") ?: 0L)
                    put("note", doc.getString("note") ?: "")
                    put("paymentMethod", doc.getString("paymentMethod") ?: "cash")
                    put("isDemo", doc.getBoolean("isDemo") ?: false)
                })
            }
            root.put("transactions", txArr)

            val budArr = JSONArray()
            budgetsCol(userId).get().await().documents.forEach { doc ->
                budArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("categoryId", doc.getString("categoryId") ?: "")
                    put("categoryNameBn", doc.getString("categoryNameBn") ?: "")
                    put("categoryNameEn", doc.getString("categoryNameEn") ?: "")
                    put("monthKey", doc.getString("monthKey") ?: DefaultCatalog.currentMonthKey())
                    put("limitAmount", doc.getDouble("limitAmount") ?: 0.0)
                    put("isDemo", doc.getBoolean("isDemo") ?: false)
                })
            }
            root.put("budgets", budArr)

            val goalsArr = JSONArray()
            savingsGoalsCol(userId).get().await().documents.forEach { doc ->
                goalsArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.getString("title") ?: "")
                    put("targetAmount", doc.getDouble("targetAmount") ?: 0.0)
                    put("savedAmount", doc.getDouble("savedAmount") ?: 0.0)
                    put("targetDateMillis", doc.getLong("targetDateMillis") ?: 0L)
                    put("iconKey", doc.getString("iconKey") ?: "savings")
                    put("colorHex", doc.getLong("colorHex") ?: 0xFF10B981L)
                    put("isDemo", doc.getBoolean("isDemo") ?: false)
                })
            }
            root.put("savingsGoals", goalsArr)

            val seArr = JSONArray()
            savingsEntriesCol(userId).get().await().documents.forEach { doc ->
                seArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("goalId", doc.getString("goalId") ?: "")
                    put("goalTitle", doc.getString("goalTitle") ?: "")
                    put("amount", doc.getDouble("amount") ?: 0.0)
                    put("note", doc.getString("note") ?: "")
                    put("timestampMillis", doc.getLong("timestampMillis") ?: 0L)
                    put("isDemo", doc.getBoolean("isDemo") ?: false)
                })
            }
            root.put("savingsEntries", seArr)

            val lbArr = JSONArray()
            lendBorrowsCol(userId).get().await().documents.forEach { doc ->
                lbArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("kind", doc.getString("kind") ?: "lend")
                    put("personName", doc.getString("personName") ?: "")
                    put("phoneNumber", doc.getString("phoneNumber") ?: "")
                    put("totalAmount", doc.getDouble("totalAmount") ?: 0.0)
                    put("paidAmount", doc.getDouble("paidAmount") ?: 0.0)
                    put("dateMillis", doc.getLong("dateMillis") ?: 0L)
                    put("dueDateMillis", doc.getLong("dueDateMillis") ?: 0L)
                    put("description", doc.getString("description") ?: "")
                    put("status", doc.getString("status") ?: "unpaid")
                    put("isDemo", doc.getBoolean("isDemo") ?: false)
                })
            }
            root.put("lendBorrows", lbArr)

            val lbpArr = JSONArray()
            lendBorrowPaymentsCol(userId).get().await().documents.forEach { doc ->
                lbpArr.put(JSONObject().apply {
                    put("id", doc.id)
                    put("recordId", doc.getString("recordId") ?: "")
                    put("amount", doc.getDouble("amount") ?: 0.0)
                    put("note", doc.getString("note") ?: "")
                    put("timestampMillis", doc.getLong("timestampMillis") ?: 0L)
                    put("isDemo", doc.getBoolean("isDemo") ?: false)
                })
            }
            root.put("lendBorrowPayments", lbpArr)

            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, userDoc(userId).path, auth)
            Result.failure(e)
        }
    }

    override suspend fun restoreBackupJson(userId: String, jsonText: String): Result<Int> {
        return try {
            val root = JSONObject(jsonText)
            val parentOps = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            val childOps = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()

            root.optJSONArray("categories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: "cat_${UUID.randomUUID().toString().take(10)}"
                    parentOps.add(
                        categoriesCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "nameBn" to obj.optString("nameBn", "ক্যাটাগরি").take(60).ifBlank { "ক্যাটাগরি" },
                            "nameEn" to obj.optString("nameEn", "Category").take(60).ifBlank { "Category" },
                            "type" to if (obj.optString("type") == "income") "income" else "expense",
                            "iconKey" to obj.optString("iconKey", "category").take(40).ifBlank { "category" },
                            "colorHex" to obj.optLong("colorHex", 0xFFFF6B2CL),
                            "isDefault" to obj.optBoolean("isDefault", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            root.optJSONArray("transactions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val amount = obj.optDouble("amount", 0.0)
                    if (amount <= 0.0) continue
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: UUID.randomUUID().toString()
                    val method = obj.optString("paymentMethod", "cash").let {
                        if (it in listOf("cash", "bank", "mobile_banking", "other")) it else "cash"
                    }
                    parentOps.add(
                        transactionsCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "type" to if (obj.optString("type") == "income") "income" else "expense",
                            "amount" to amount,
                            "categoryId" to obj.optString("categoryId", "exp_other").ifBlank { "exp_other" },
                            "categoryNameBn" to obj.optString("categoryNameBn", "অন্যান্য").ifBlank { "অন্যান্য" },
                            "categoryNameEn" to obj.optString("categoryNameEn", "Other").ifBlank { "Other" },
                            "timestampMillis" to obj.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                            "note" to obj.optString("note", "").take(300),
                            "paymentMethod" to method,
                            "isDemo" to obj.optBoolean("isDemo", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            root.optJSONArray("budgets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val limit = obj.optDouble("limitAmount", 0.0)
                    if (limit <= 0.0) continue
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: "bud_${UUID.randomUUID().toString().take(10)}"
                    val monthKey = obj.optString("monthKey", DefaultCatalog.currentMonthKey())
                        .takeIf { it.matches(Regex("^[0-9]{4}-[0-9]{2}$")) }
                        ?: DefaultCatalog.currentMonthKey()
                    parentOps.add(
                        budgetsCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "categoryId" to obj.optString("categoryId", "exp_other").ifBlank { "exp_other" },
                            "categoryNameBn" to obj.optString("categoryNameBn", "অন্যান্য").ifBlank { "অন্যান্য" },
                            "categoryNameEn" to obj.optString("categoryNameEn", "Other").ifBlank { "Other" },
                            "monthKey" to monthKey,
                            "limitAmount" to limit,
                            "isDemo" to obj.optBoolean("isDemo", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val validGoalIds = mutableSetOf<String>()
            root.optJSONArray("savingsGoals")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val target = obj.optDouble("targetAmount", 0.0)
                    if (target <= 0.0) continue
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: "goal_${UUID.randomUUID().toString().take(10)}"
                    validGoalIds.add(id)
                    parentOps.add(
                        savingsGoalsCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "title" to obj.optString("title", "Savings Goal").take(80).ifBlank { "Savings Goal" },
                            "targetAmount" to target,
                            "savedAmount" to obj.optDouble("savedAmount", 0.0).coerceAtLeast(0.0),
                            "targetDateMillis" to obj.optLong("targetDateMillis", 0L).coerceAtLeast(0L),
                            "iconKey" to obj.optString("iconKey", "savings").ifBlank { "savings" },
                            "colorHex" to obj.optLong("colorHex", 0xFF10B981L),
                            "isDemo" to obj.optBoolean("isDemo", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            root.optJSONArray("savingsEntries")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val amt = obj.optDouble("amount", 0.0)
                    val goalId = obj.optString("goalId")
                    if (amt <= 0.0 || !goalId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) || goalId !in validGoalIds) continue
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: "se_${UUID.randomUUID().toString().take(10)}"
                    childOps.add(
                        savingsEntriesCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "goalId" to goalId,
                            "goalTitle" to obj.optString("goalTitle", "Savings").take(80).ifBlank { "Savings" },
                            "amount" to amt,
                            "note" to obj.optString("note", "").take(200),
                            "timestampMillis" to obj.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                            "isDemo" to obj.optBoolean("isDemo", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val validLbIds = mutableSetOf<String>()
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
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: "lb_${UUID.randomUUID().toString().take(10)}"
                    validLbIds.add(id)
                    parentOps.add(
                        lendBorrowsCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "kind" to if (obj.optString("kind") == "borrow") "borrow" else "lend",
                            "personName" to obj.optString("personName", "Person").take(80).ifBlank { "Person" },
                            "phoneNumber" to obj.optString("phoneNumber", "").take(30),
                            "totalAmount" to total,
                            "paidAmount" to paid,
                            "dateMillis" to obj.optLong("dateMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                            "dueDateMillis" to obj.optLong("dueDateMillis", 0L).coerceAtLeast(0L),
                            "description" to obj.optString("description", "").take(300),
                            "status" to status,
                            "isDemo" to obj.optBoolean("isDemo", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            root.optJSONArray("lendBorrowPayments")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val amt = obj.optDouble("amount", 0.0)
                    val recId = obj.optString("recordId")
                    if (amt <= 0.0 || !recId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) || recId !in validLbIds) continue
                    val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                        ?: "lbp_${UUID.randomUUID().toString().take(10)}"
                    childOps.add(
                        lendBorrowPaymentsCol(userId).document(id) to mapOf(
                            "userId" to userId,
                            "recordId" to recId,
                            "amount" to amt,
                            "note" to obj.optString("note", "").take(200),
                            "timestampMillis" to obj.optLong("timestampMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                            "isDemo" to obj.optBoolean("isDemo", false),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            parentOps.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, data) -> batch.set(ref, data) }
                batch.commit().await()
            }
            childOps.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, data) -> batch.set(ref, data) }
                batch.commit().await()
            }
            Result.success(parentOps.size + childOps.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc(userId).path, auth)
            Result.failure(e)
        }
    }
}
