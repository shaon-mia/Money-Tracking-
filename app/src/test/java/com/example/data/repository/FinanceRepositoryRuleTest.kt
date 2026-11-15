package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.FinanceTransaction
import com.example.data.model.LendBorrowRecord
import com.example.data.model.SavingsGoal
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun seedAndCrud_authenticatedOwner_succeedsAndStreamsUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FinanceRepository(firestore, auth)

        val seedRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.seedInitialDataIfNeeded(uid) }
        assertTrue("Seed failed: ${seedRes.exceptionOrNull()}", seedRes.isSuccess)

        val categories = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCategories(uid).first { it.isNotEmpty() }
        }
        assertTrue(categories.any { it.id == "exp_food" && it.isDefault })

        val txId = "test_tx_${UUID.randomUUID().toString().take(8)}"
        val createTx = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertTransaction(
                uid,
                FinanceTransaction(
                    type = "expense",
                    amount = 350.0,
                    categoryId = "exp_food",
                    categoryNameBn = "খাবার",
                    categoryNameEn = "Food",
                    timestampMillis = System.currentTimeMillis(),
                    note = "নাস্তা",
                    paymentMethod = "cash",
                    isDemo = false
                ),
                documentId = txId
            )
        }
        assertTrue("Create tx failed: ${createTx.exceptionOrNull()}", createTx.isSuccess)

        val txs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTransactions(uid).first { list -> list.any { it.id == txId } }
        }
        assertEquals(350.0, txs.first { it.id == txId }.amount, 0.01)

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertSavingsGoal(
                uid,
                SavingsGoal(
                    title = "Test Goal",
                    targetAmount = 10000.0,
                    savedAmount = 1000.0,
                    targetDateMillis = System.currentTimeMillis() + 86400000L
                )
            ).getOrThrow()
        }
        val goal = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSavingsGoals(uid).first { list -> list.any { it.id == goalId } }.first { it.id == goalId }
        }
        val contribRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addSavingsContribution(uid, goal, 500.0, "Added")
        }
        assertTrue("Contribution failed: ${contribRes.exceptionOrNull()}", contribRes.isSuccess)

        val lbId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertLendBorrow(
                uid,
                LendBorrowRecord(
                    kind = "lend",
                    personName = "Hasan",
                    totalAmount = 2000.0,
                    paidAmount = 0.0,
                    dateMillis = System.currentTimeMillis()
                )
            ).getOrThrow()
        }
        val lb = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeLendBorrows(uid).first { list -> list.any { it.id == lbId } }.first { it.id == lbId }
        }
        val payRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addLendBorrowPayment(uid, lb, 1000.0, "Half")
        }
        assertTrue("Payment failed: ${payRes.exceptionOrNull()}", payRes.isSuccess)

        val catRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertCategory(
                uid,
                CategoryItem(nameBn = "নতুন", nameEn = "Custom", type = "expense", iconKey = "star")
            )
        }
        assertTrue(catRes.isSuccess)

        val budRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertBudget(
                uid,
                BudgetItem(
                    categoryId = "exp_food",
                    categoryNameBn = "খাবার",
                    categoryNameEn = "Food",
                    monthKey = "2026-11",
                    limitAmount = 8000.0
                )
            )
        }
        assertTrue(budRes.isSuccess)
    }

    @Test
    fun crossUserAccess_bobCannotReadAliceTransactions() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FinanceRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.seedInitialDataIfNeeded(aliceUid).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FinanceRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's transactions")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = FinanceRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTransactions(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@hisabkori.test"
        const val BOB_EMAIL = "bob@hisabkori.test"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
