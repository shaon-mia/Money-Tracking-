package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserPreferences(
    @DocumentId val id: String = "preferences",
    val userId: String = "",
    val language: String = "bn",
    val themeMode: String = "light",
    val textScale: String = "medium",
    val currencyCode: String = "BDT",
    val monthlySavingsGoal: Double = 10000.0,
    val biometricLockEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinHash: String? = null,
    val deletePasswordEnabled: Boolean = false,
    val deletePasswordHash: String? = null,
    val hideBalances: Boolean = false,
    val demoSeeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CategoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val type: String = "expense", // "income" | "expense"
    val iconKey: String = "category",
    val colorHex: Long = 0xFFFF6B2CL,
    @get:PropertyName("isDefault") @set:PropertyName("isDefault") var isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayName(language: String): String =
        if (language == "bn") nameBn.ifBlank { nameEn } else nameEn.ifBlank { nameBn }
}

@IgnoreExtraProperties
data class FinanceTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "expense", // "income" | "expense"
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryNameBn: String = "",
    val categoryNameEn: String = "",
    val timestampMillis: Long = 0L,
    val note: String = "",
    val paymentMethod: String = "cash", // "cash" | "bank" | "mobile_banking" | "other"
    val photoUri: String? = null,
    val clientRequestId: String? = null,
    @get:PropertyName("isDemo") @set:PropertyName("isDemo") var isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryDisplayName(language: String): String =
        if (language == "bn") categoryNameBn.ifBlank { categoryNameEn } else categoryNameEn.ifBlank { categoryNameBn }
}

@IgnoreExtraProperties
data class BudgetItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val categoryNameBn: String = "",
    val categoryNameEn: String = "",
    val monthKey: String = "", // YYYY-MM
    val limitAmount: Double = 0.0,
    @get:PropertyName("isDemo") @set:PropertyName("isDemo") var isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryDisplayName(language: String): String =
        if (language == "bn") categoryNameBn.ifBlank { categoryNameEn } else categoryNameEn.ifBlank { categoryNameBn }
}

@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val targetDateMillis: Long = 0L,
    val iconKey: String = "savings",
    val colorHex: Long = 0xFF10B981L,
    @get:PropertyName("isDemo") @set:PropertyName("isDemo") var isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavingsEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goalId: String = "",
    val goalTitle: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val timestampMillis: Long = 0L,
    @get:PropertyName("isDemo") @set:PropertyName("isDemo") var isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LendBorrowRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "lend", // "lend" (receive) | "borrow" (repay)
    val personName: String = "",
    val phoneNumber: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dateMillis: Long = 0L,
    val dueDateMillis: Long = 0L,
    val description: String = "",
    val status: String = "unpaid", // "unpaid" | "partial" | "paid"
    @get:PropertyName("isDemo") @set:PropertyName("isDemo") var isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val remainingAmount: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
}

@IgnoreExtraProperties
data class LendBorrowPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recordId: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val timestampMillis: Long = 0L,
    @get:PropertyName("isDemo") @set:PropertyName("isDemo") var isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
