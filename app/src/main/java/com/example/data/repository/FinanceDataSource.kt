package com.example.data.repository

import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.FinanceTransaction
import com.example.data.model.LendBorrowPayment
import com.example.data.model.LendBorrowRecord
import com.example.data.model.SavingsEntry
import com.example.data.model.SavingsGoal
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface FinanceDataSource {
    fun observePreferences(userId: String): Flow<UserPreferences>
    fun observeCategories(userId: String): Flow<List<CategoryItem>>
    fun observeTransactions(userId: String): Flow<List<FinanceTransaction>>
    fun observeBudgets(userId: String): Flow<List<BudgetItem>>
    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>>
    fun observeSavingsEntries(userId: String): Flow<List<SavingsEntry>>
    fun observeLendBorrows(userId: String): Flow<List<LendBorrowRecord>>
    fun observeLendBorrowPayments(userId: String): Flow<List<LendBorrowPayment>>

    suspend fun savePreferences(userId: String, prefs: UserPreferences): Result<Unit>

    suspend fun upsertTransaction(
        userId: String,
        transaction: FinanceTransaction,
        documentId: String? = null
    ): Result<String>
    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit>

    suspend fun upsertCategory(userId: String, category: CategoryItem): Result<String>
    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit>

    suspend fun upsertBudget(userId: String, budget: BudgetItem): Result<String>
    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit>

    suspend fun upsertSavingsGoal(userId: String, goal: SavingsGoal): Result<String>
    suspend fun addSavingsContribution(
        userId: String,
        goal: SavingsGoal,
        amount: Double,
        note: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Result<Unit>
    suspend fun deleteSavingsGoal(userId: String, goalId: String): Result<Unit>

    suspend fun upsertLendBorrow(userId: String, record: LendBorrowRecord): Result<String>
    suspend fun addLendBorrowPayment(
        userId: String,
        record: LendBorrowRecord,
        paymentAmount: Double,
        note: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Result<Unit>
    suspend fun deleteLendBorrow(userId: String, recordId: String): Result<Unit>

    suspend fun seedInitialDataIfNeeded(userId: String): Result<Unit>
    suspend fun syncMissingDefaultCategories(userId: String): Result<Int>
    suspend fun clearDemoData(userId: String): Result<Unit>
    suspend fun exportBackupJson(userId: String): Result<String>
    suspend fun restoreBackupJson(userId: String, jsonText: String): Result<Int>
}
