package com.example.util

import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCatalog
import com.example.data.model.FinanceTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun computeDashboardTotals_andBudgetAlerts_areAccurate() {
        val now = System.currentTimeMillis()
        val monthKey = DefaultCatalog.currentMonthKey(now)
        val txs = listOf(
            FinanceTransaction(
                id = "t1",
                type = "income",
                amount = 50000.0,
                categoryId = "inc_salary",
                categoryNameBn = "বেতন",
                categoryNameEn = "Salary",
                timestampMillis = now
            ),
            FinanceTransaction(
                id = "t2",
                type = "expense",
                amount = 4500.0,
                categoryId = "exp_food",
                categoryNameBn = "খাবার",
                categoryNameEn = "Food",
                timestampMillis = now
            ),
            FinanceTransaction(
                id = "t3",
                type = "expense",
                amount = 3500.0,
                categoryId = "exp_shopping",
                categoryNameBn = "শপিং",
                categoryNameEn = "Shopping",
                timestampMillis = now - 86_400_000L
            )
        )
        val budgets = listOf(
            BudgetItem(
                id = "b1",
                categoryId = "exp_food",
                categoryNameBn = "খাবার",
                categoryNameEn = "Food",
                monthKey = monthKey,
                limitAmount = 5000.0
            ),
            BudgetItem(
                id = "b2",
                categoryId = "exp_shopping",
                categoryNameBn = "শপিং",
                categoryNameEn = "Shopping",
                monthKey = monthKey,
                limitAmount = 3000.0
            )
        )

        val totals = FinancialCalculator.computeDashboardTotals(txs, budgets, now)
        assertEquals(50000.0, totals.todayIncome, 0.01)
        assertEquals(4500.0, totals.todayExpense, 0.01)
        assertEquals(42000.0, totals.currentBalance, 0.01)
        assertEquals(50000.0, totals.monthlyIncome, 0.01)
        assertEquals(8000.0, totals.monthlyExpense, 0.01)
        assertEquals(42000.0, totals.monthlySavings, 0.01)
        assertEquals(0.0, totals.remainingMonthlyBudget, 0.01)

        val statuses = FinancialCalculator.computeBudgetStatuses(budgets, txs, monthKey)
        val foodStatus = statuses.first { it.budget.categoryId == "exp_food" }
        val shoppingStatus = statuses.first { it.budget.categoryId == "exp_shopping" }
        assertTrue(foodStatus.isWarning80)
        assertTrue(shoppingStatus.isExceeded)

        val cats = DefaultCatalog.allDefaultCategories.map {
            CategoryItem(id = it.id, nameBn = it.nameBn, nameEn = it.nameEn, type = it.type, iconKey = it.iconKey, colorHex = it.colorHex)
        }
        val insights = FinancialCalculator.generateSmartInsights(txs, cats, budgets, "BDT", "bn", now)
        assertTrue(insights.isNotEmpty())
    }
}
