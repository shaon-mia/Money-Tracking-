package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.FinanceTransaction
import com.example.data.model.LendBorrowPayment
import com.example.data.model.LendBorrowRecord
import com.example.data.model.SavingsEntry
import com.example.data.model.SavingsGoal
import com.example.data.model.UserPreferences
import com.example.data.repository.FinanceDataSource
import com.example.util.ReportExporter
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceDashboardState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<CategoryItem> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val budgets: List<BudgetItem> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val savingsEntries: List<SavingsEntry> = emptyList(),
    val lendBorrows: List<LendBorrowRecord> = emptyList(),
    val lendBorrowPayments: List<LendBorrowPayment> = emptyList()
) {
    val hasDemoData: Boolean
        get() = transactions.any { it.isDemo } ||
            budgets.any { it.isDemo } ||
            savingsGoals.any { it.isDemo } ||
            lendBorrows.any { it.isDemo }
}

class FinanceViewModel(
    private val repository: FinanceDataSource,
    val currentUserId: String
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val uiState: StateFlow<FinanceDashboardState> = combine(
        repository.observePreferences(currentUserId),
        repository.observeCategories(currentUserId),
        repository.observeTransactions(currentUserId),
        repository.observeBudgets(currentUserId),
        combine(
            repository.observeSavingsGoals(currentUserId),
            repository.observeSavingsEntries(currentUserId),
            repository.observeLendBorrows(currentUserId),
            repository.observeLendBorrowPayments(currentUserId)
        ) { goals, entries, lendBorrows, payments ->
            QuadData(goals, entries, lendBorrows, payments)
        }
    ) { prefs, cats, txs, budgets, quad ->
        FinanceDashboardState(
            isLoading = false,
            errorMessage = null,
            preferences = prefs,
            categories = cats,
            transactions = txs,
            budgets = budgets,
            savingsGoals = quad.goals,
            savingsEntries = quad.entries,
            lendBorrows = quad.lendBorrows,
            lendBorrowPayments = quad.payments
        )
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing finance streams", rootError)
        emit(
            FinanceDashboardState(
                isLoading = false,
                errorMessage = rootError.message ?: "Failed to sync data"
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FinanceDashboardState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded(currentUserId)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = uiState.value.preferences.pinHash
        val matches = storedHash != null && ReportExporter.sha256(pin) == storedHash
        if (matches) {
            _isUnlocked.value = true
        }
        return matches
    }

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val current = uiState.value.preferences
        val updated = transform(current)
        viewModelScope.launch {
            val res = repository.savePreferences(currentUserId, updated)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save settings"
            }
        }
    }

    fun setPin(pin: String) {
        val hash = ReportExporter.sha256(pin)
        updatePreferences { it.copy(pinEnabled = true, pinHash = hash) }
        _isUnlocked.value = true
    }

    fun disablePin() {
        updatePreferences { it.copy(pinEnabled = false, pinHash = null, biometricLockEnabled = false) }
    }

    fun setDeletePassword(password: String) {
        val hash = ReportExporter.sha256(password)
        updatePreferences { it.copy(deletePasswordEnabled = true, deletePasswordHash = hash) }
    }

    fun verifyDeletePassword(password: String): Boolean {
        val storedHash = uiState.value.preferences.deletePasswordHash
        return !storedHash.isNullOrBlank() && ReportExporter.sha256(password) == storedHash
    }

    fun disableDeletePassword() {
        updatePreferences { it.copy(deletePasswordEnabled = false, deletePasswordHash = null) }
    }

    fun saveTransaction(
        tx: FinanceTransaction,
        documentId: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            val res = repository.upsertTransaction(currentUserId, tx, documentId)
            _isSaving.value = false
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save transaction"
            }
            onComplete(res.isSuccess)
        }
    }

    fun deleteTransaction(txId: String) {
        viewModelScope.launch {
            val res = repository.deleteTransaction(currentUserId, txId)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete"
            }
        }
    }

    fun saveCategory(category: CategoryItem, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.upsertCategory(currentUserId, category)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save category"
            }
            onComplete(res.isSuccess)
        }
    }

    fun saveCategoryWithResult(category: CategoryItem, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.upsertCategory(currentUserId, category)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save category"
            }
            onComplete(res.getOrNull())
        }
    }

    fun syncMissingDefaultCategories() {
        viewModelScope.launch {
            repository.syncMissingDefaultCategories(currentUserId)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val res = repository.deleteCategory(currentUserId, categoryId)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete category"
            }
        }
    }

    fun saveBudget(budget: BudgetItem, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.upsertBudget(currentUserId, budget)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save budget"
            }
            onComplete(res.isSuccess)
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(currentUserId, budgetId)
        }
    }

    fun saveSavingsGoal(goal: SavingsGoal, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.upsertSavingsGoal(currentUserId, goal)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save savings goal"
            }
            onComplete(res.isSuccess)
        }
    }

    fun addSavingsContribution(goal: SavingsGoal, amount: Double, note: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.addSavingsContribution(currentUserId, goal, amount, note)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to add savings"
            }
            onComplete(res.isSuccess)
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(currentUserId, goalId)
        }
    }

    fun saveLendBorrow(record: LendBorrowRecord, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.upsertLendBorrow(currentUserId, record)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save record"
            }
            onComplete(res.isSuccess)
        }
    }

    fun addLendBorrowPayment(
        record: LendBorrowRecord,
        amount: Double,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.addLendBorrowPayment(currentUserId, record, amount, note)
            if (res.isFailure) {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to record payment"
            }
            onComplete(res.isSuccess)
        }
    }

    fun deleteLendBorrow(recordId: String) {
        viewModelScope.launch {
            repository.deleteLendBorrow(currentUserId, recordId)
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            val res = repository.clearDemoData(currentUserId)
            val isBn = uiState.value.preferences.language == "bn"
            _snackbarMessage.value = if (res.isSuccess) {
                if (isBn) "ডেমো ডাটা মুছে ফেলা হয়েছে" else "Demo data cleared"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed"
            }
        }
    }

    suspend fun exportBackupJson(): Result<String> = repository.exportBackupJson(currentUserId)

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            val res = repository.restoreBackupJson(currentUserId, jsonText)
            val isBn = uiState.value.preferences.language == "bn"
            _snackbarMessage.value = if (res.isSuccess) {
                val count = res.getOrDefault(0)
                if (isBn) "$count টি রেকর্ড রিস্টোর করা হয়েছে" else "Restored $count records"
            } else {
                if (isBn) "ব্যাকআপ ফাইল সঠিক নয়" else "Invalid backup file"
            }
        }
    }

    private data class QuadData(
        val goals: List<SavingsGoal>,
        val entries: List<SavingsEntry>,
        val lendBorrows: List<LendBorrowRecord>,
        val payments: List<LendBorrowPayment>
    )

    private companion object {
        const val TAG = "FinanceVM"
    }
}
