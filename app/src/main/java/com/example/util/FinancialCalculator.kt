package com.example.util

import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCatalog
import com.example.data.model.FinanceTransaction
import java.util.Calendar
import kotlin.math.abs

data class CategoryBreakdown(
    val categoryId: String,
    val nameBn: String,
    val nameEn: String,
    val colorHex: Long,
    val iconKey: String,
    val amount: Double,
    val percentage: Float
) {
    fun displayName(language: String): String =
        if (language == "bn") nameBn.ifBlank { nameEn } else nameEn.ifBlank { nameBn }
}

data class DailyPoint(
    val label: String,
    val dateMillis: Long,
    val income: Double,
    val expense: Double
)

data class BudgetStatus(
    val budget: BudgetItem,
    val spent: Double,
    val remaining: Double,
    val progress: Float,
    val isWarning80: Boolean,
    val isExceeded: Boolean
)

data class SmartInsight(
    val title: String,
    val description: String,
    val tone: InsightTone
)

enum class InsightTone { POSITIVE, WARNING, ALERT, INFO }

object FinancialCalculator {

    fun startOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(timeMillis: Long): Long = startOfDayMillis(timeMillis) + 86_400_000L - 1L

    fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    fun computeDashboardTotals(
        transactions: List<FinanceTransaction>,
        budgets: List<BudgetItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardTotals {
        val currentMonth = DefaultCatalog.currentMonthKey(nowMillis)
        var todayInc = 0.0
        var todayExp = 0.0
        var totalInc = 0.0
        var totalExp = 0.0
        var monthInc = 0.0
        var monthExp = 0.0

        transactions.forEach { tx ->
            if (tx.type == "income") {
                totalInc += tx.amount
                if (isSameDay(tx.timestampMillis, nowMillis)) todayInc += tx.amount
                if (DefaultCatalog.monthKeyOf(tx.timestampMillis) == currentMonth) monthInc += tx.amount
            } else {
                totalExp += tx.amount
                if (isSameDay(tx.timestampMillis, nowMillis)) todayExp += tx.amount
                if (DefaultCatalog.monthKeyOf(tx.timestampMillis) == currentMonth) monthExp += tx.amount
            }
        }

        val currentMonthBudgets = budgets.filter { it.monthKey == currentMonth }
        val totalMonthlyBudget = currentMonthBudgets.sumOf { it.limitAmount }
        val remainingBudget = (totalMonthlyBudget - monthExp).coerceAtLeast(0.0)

        return DashboardTotals(
            todayIncome = todayInc,
            todayExpense = todayExp,
            currentBalance = totalInc - totalExp,
            monthlyIncome = monthInc,
            monthlyExpense = monthExp,
            monthlySavings = monthInc - monthExp,
            totalMonthlyBudget = totalMonthlyBudget,
            remainingMonthlyBudget = remainingBudget
        )
    }

    fun computeBudgetStatuses(
        budgets: List<BudgetItem>,
        transactions: List<FinanceTransaction>,
        monthKey: String
    ): List<BudgetStatus> {
        val monthExpensesByCategory = transactions
            .filter { it.type == "expense" && DefaultCatalog.monthKeyOf(it.timestampMillis) == monthKey }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        return budgets
            .filter { it.monthKey == monthKey }
            .map { budget ->
                val spent = monthExpensesByCategory[budget.categoryId] ?: 0.0
                val remaining = budget.limitAmount - spent
                val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                BudgetStatus(
                    budget = budget,
                    spent = spent,
                    remaining = remaining,
                    progress = progress,
                    isWarning80 = progress >= 0.8f && progress <= 1.0f,
                    isExceeded = progress > 1.0f
                )
            }
            .sortedByDescending { it.progress }
    }

    fun computeCategoryBreakdown(
        transactions: List<FinanceTransaction>,
        categories: List<CategoryItem>,
        type: String = "expense"
    ): List<CategoryBreakdown> {
        val filtered = transactions.filter { it.type == type }
        val total = filtered.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        val catMap = categories.associateBy { it.id }
        return filtered
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val sum = list.sumOf { it.amount }
                val cat = catMap[catId]
                val first = list.first()
                CategoryBreakdown(
                    categoryId = catId,
                    nameBn = cat?.nameBn ?: first.categoryNameBn,
                    nameEn = cat?.nameEn ?: first.categoryNameEn,
                    colorHex = cat?.colorHex ?: 0xFFFF6B2CL,
                    iconKey = cat?.iconKey ?: "category",
                    amount = sum,
                    percentage = ((sum / total) * 100.0).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }

    fun computeDailySeries(
        transactions: List<FinanceTransaction>,
        days: Int = 7,
        endMillis: Long = System.currentTimeMillis(),
        language: String = "bn"
    ): List<DailyPoint> {
        val result = mutableListOf<DailyPoint>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(endMillis)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        val bnShortDays = mapOf(
            Calendar.SATURDAY to "শনি",
            Calendar.SUNDAY to "রবি",
            Calendar.MONDAY to "সোম",
            Calendar.TUESDAY to "মঙ্গল",
            Calendar.WEDNESDAY to "বুধ",
            Calendar.THURSDAY to "বৃহঃ",
            Calendar.FRIDAY to "শুক্র"
        )
        val enShortDays = mapOf(
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri"
        )
        repeat(days) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 86_400_000L - 1L
            val dayTxs = transactions.filter { it.timestampMillis in dayStart..dayEnd }
            val inc = dayTxs.filter { it.type == "income" }.sumOf { it.amount }
            val exp = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
            val label = if (days <= 7) {
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                if (language == "bn") bnShortDays[dow] ?: "" else enShortDays[dow] ?: ""
            } else {
                "${cal.get(Calendar.DAY_OF_MONTH)}"
            }
            result.add(DailyPoint(label, dayStart, inc, exp))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun generateSmartInsights(
        transactions: List<FinanceTransaction>,
        categories: List<CategoryItem>,
        budgets: List<BudgetItem>,
        currencyCode: String,
        language: String,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SmartInsight> {
        val isBn = language == "bn"
        val currentMonth = DefaultCatalog.currentMonthKey(nowMillis)
        val prevMonth = DefaultCatalog.previousMonthKey(nowMillis)

        val currentTxs = transactions.filter { DefaultCatalog.monthKeyOf(it.timestampMillis) == currentMonth }
        val prevTxs = transactions.filter { DefaultCatalog.monthKeyOf(it.timestampMillis) == prevMonth }

        val curIncome = currentTxs.filter { it.type == "income" }.sumOf { it.amount }
        val curExpense = currentTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val prevExpense = prevTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val curSavings = curIncome - curExpense

        val insights = mutableListOf<SmartInsight>()
        val symbol = com.example.ui.i18n.CurrencyCatalog.symbolFor(currencyCode)

        // 1. Highest spending category
        val breakdown = computeCategoryBreakdown(currentTxs, categories, "expense")
        val topCat = breakdown.firstOrNull()
        if (topCat != null) {
            val pct = topCat.percentage.toInt()
            insights.add(
                SmartInsight(
                    title = if (isBn) "সর্বোচ্চ খরচের খাত: ${topCat.nameBn}" else "Top Spending: ${topCat.nameEn}",
                    description = if (isBn) {
                        "এই মাসে মোট খরচের $pct% ($symbol ${topCat.amount.toInt()}) '${topCat.nameBn}' খাতে ব্যয় হয়েছে।"
                    } else {
                        "${topCat.nameEn} accounts for $pct% ($symbol ${topCat.amount.toInt()}) of your spending this month."
                    },
                    tone = InsightTone.INFO
                )
            )
        }

        // 2. Discretionary cutback suggestion
        val discretionaryIds = setOf("exp_shopping", "exp_entertainment", "exp_food", "exp_personal")
        val topDiscretionary = breakdown.firstOrNull { it.categoryId in discretionaryIds }
        if (topDiscretionary != null && topDiscretionary.amount > 0.0) {
            val savePotential = (topDiscretionary.amount * 0.20).toInt()
            insights.add(
                SmartInsight(
                    title = if (isBn) "খরচ কমানোর সুযোগ" else "Savings Opportunity",
                    description = if (isBn) {
                        "'${topDiscretionary.nameBn}' খাতে ২০% খরচ কমাতে পারলে প্রতি মাসে প্রায় $symbol $savePotential অতিরিক্ত সঞ্চয় সম্ভব।"
                    } else {
                        "Trimming '${topDiscretionary.nameEn}' by 20% could save roughly $symbol $savePotential every month."
                    },
                    tone = InsightTone.WARNING
                )
            )
        }

        // 3. Budget status
        val budgetStatuses = computeBudgetStatuses(budgets, transactions, currentMonth)
        val exceeded = budgetStatuses.filter { it.isExceeded }
        val nearLimit = budgetStatuses.filter { it.isWarning80 }
        when {
            exceeded.isNotEmpty() -> {
                val names = exceeded.joinToString(", ") { it.budget.categoryDisplayName(language) }
                insights.add(
                    SmartInsight(
                        title = if (isBn) "বাজেট সীমা অতিক্রম সতর্কতা" else "Over-Budget Alert",
                        description = if (isBn) {
                            "$names খাতে নির্ধারিত মাসিক বাজেট অতিক্রম হয়েছে।"
                        } else {
                            "Monthly budget limit exceeded in: $names."
                        },
                        tone = InsightTone.ALERT
                    )
                )
            }
            nearLimit.isNotEmpty() -> {
                val names = nearLimit.joinToString(", ") { it.budget.categoryDisplayName(language) }
                insights.add(
                    SmartInsight(
                        title = if (isBn) "বাজেটের ৮০% পূর্ণ হয়েছে" else "Approaching Budget Limit",
                        description = if (isBn) {
                            "$names খাতে বাজেটের ৮০% এর বেশি খরচ হয়ে গেছে। সতর্ক থাকুন।"
                        } else {
                            "Over 80% of budget used in: $names."
                        },
                        tone = InsightTone.WARNING
                    )
                )
            }
            budgetStatuses.isNotEmpty() -> {
                insights.add(
                    SmartInsight(
                        title = if (isBn) "বাজেট নিয়ন্ত্রণে আছে" else "Within Monthly Budget",
                        description = if (isBn) {
                            "সব নির্ধারিত ক্যাটাগরির খরচ আপনার বাজেট সীমার মধ্যে রয়েছে।"
                        } else {
                            "All tracked categories are safely within your monthly budget."
                        },
                        tone = InsightTone.POSITIVE
                    )
                )
            }
        }

        // 4. Savings summary
        if (curIncome > 0.0 || curExpense > 0.0) {
            val rate = if (curIncome > 0.0) ((curSavings / curIncome) * 100).toInt() else 0
            insights.add(
                SmartInsight(
                    title = if (isBn) "মাসিক সঞ্চয় পরিস্থিতি" else "Monthly Savings Snapshot",
                    description = if (curSavings >= 0) {
                        if (isBn) "এই মাসে আপনার নিট সঞ্চয় $symbol ${curSavings.toInt()} (আয়ের $rate%)।"
                        else "You have saved $symbol ${curSavings.toInt()} ($rate% of income) this month."
                    } else {
                        if (isBn) "এই মাসে আয়ের তুলনায় $symbol ${abs(curSavings).toInt()} বেশি খরচ হয়েছে।"
                        else "Expenses exceed income by $symbol ${abs(curSavings).toInt()} this month."
                    },
                    tone = if (curSavings >= 0) InsightTone.POSITIVE else InsightTone.ALERT
                )
            )
        }

        // 5. Previous vs Current month comparison
        if (prevExpense > 0.0) {
            val diff = curExpense - prevExpense
            val pctChange = ((abs(diff) / prevExpense) * 100).toInt()
            insights.add(
                SmartInsight(
                    title = if (isBn) "গত মাসের সাথে তুলনা" else "Month-over-Month Comparison",
                    description = if (diff <= 0) {
                        if (isBn) "গত মাসের ($symbol ${prevExpense.toInt()}) তুলনায় এই মাসে খরচ $pctChange% কম রয়েছে।"
                        else "Spending is $pctChange% lower compared to last month ($symbol ${prevExpense.toInt()})."
                    } else {
                        if (isBn) "গত মাসের ($symbol ${prevExpense.toInt()}) তুলনায় এই মাসে খরচ $pctChange% বেড়েছে।"
                        else "Spending is $pctChange% higher compared to last month ($symbol ${prevExpense.toInt()})."
                    },
                    tone = if (diff <= 0) InsightTone.POSITIVE else InsightTone.WARNING
                )
            )
        }

        return insights
    }

    fun parseMonthKey(monthKey: String): Pair<Int, Int>? {
        val parts = monthKey.split("-")
        if (parts.size != 2) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (m !in 1..12) return null
        return y to (m - 1)
    }

    fun adjacentMonthKey(monthKey: String, deltaMonths: Int): String {
        val parsed = parseMonthKey(monthKey) ?: return DefaultCatalog.currentMonthKey()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parsed.first)
            set(Calendar.MONTH, parsed.second)
            set(Calendar.DAY_OF_MONTH, 15)
            add(Calendar.MONTH, deltaMonths)
        }
        return String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun defaultTimestampForMonth(monthKey: String, nowMillis: Long = System.currentTimeMillis()): Long {
        if (monthKey == DefaultCatalog.currentMonthKey(nowMillis)) return nowMillis
        val parsed = parseMonthKey(monthKey) ?: return nowMillis
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parsed.first)
            set(Calendar.MONTH, parsed.second)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(15, maxDay))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun buildMonthSummary(
        monthKey: String,
        transactions: List<FinanceTransaction>,
        categories: List<CategoryItem>,
        budgets: List<BudgetItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyArchiveSummary {
        val parsed = parseMonthKey(monthKey) ?: run {
            val c = Calendar.getInstance().apply { timeInMillis = nowMillis }
            c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
        }
        val year = parsed.first
        val monthZeroBased = parsed.second
        val monthTxs = transactions
            .filter { DefaultCatalog.monthKeyOf(it.timestampMillis) == monthKey }
            .sortedByDescending { it.timestampMillis }

        val incomeTxs = monthTxs.filter { it.type == "income" }
        val expenseTxs = monthTxs.filter { it.type == "expense" }
        val totalIncome = incomeTxs.sumOf { it.amount }
        val totalExpense = expenseTxs.sumOf { it.amount }
        val budgetStatuses = computeBudgetStatuses(budgets, monthTxs, monthKey)
        val totalBudget = budgetStatuses.sumOf { it.budget.limitAmount }

        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

        val activeDays = if (monthKey == DefaultCatalog.currentMonthKey(nowMillis)) {
            Calendar.getInstance().apply { timeInMillis = nowMillis }
                .get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else {
            daysInMonth
        }

        val paymentBreakdown = expenseTxs
            .groupBy { it.paymentMethod }
            .map { (method, list) ->
                PaymentMethodBreakdown(
                    method = method,
                    amount = list.sumOf { it.amount },
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }

        return MonthlyArchiveSummary(
            monthKey = monthKey,
            year = year,
            monthZeroBased = monthZeroBased,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            totalBudget = totalBudget,
            transactionCount = monthTxs.size,
            expenseCount = expenseTxs.size,
            incomeCount = incomeTxs.size,
            averageDailyExpense = if (totalExpense > 0.0) totalExpense / activeDays else 0.0,
            topExpenseCategories = computeCategoryBreakdown(monthTxs, categories, "expense"),
            incomeBreakdown = computeCategoryBreakdown(monthTxs, categories, "income"),
            paymentBreakdown = paymentBreakdown,
            budgetStatuses = budgetStatuses,
            transactions = monthTxs
        )
    }

    fun computeMonthlyArchives(
        transactions: List<FinanceTransaction>,
        categories: List<CategoryItem>,
        budgets: List<BudgetItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<MonthlyArchiveSummary> {
        val monthKeys = linkedSetOf<String>()
        monthKeys.add(DefaultCatalog.currentMonthKey(nowMillis))
        transactions.forEach { monthKeys.add(DefaultCatalog.monthKeyOf(it.timestampMillis)) }
        budgets.forEach { if (parseMonthKey(it.monthKey) != null) monthKeys.add(it.monthKey) }

        return monthKeys
            .sortedDescending()
            .map { key ->
                buildMonthSummary(
                    monthKey = key,
                    transactions = transactions,
                    categories = categories,
                    budgets = budgets,
                    nowMillis = nowMillis
                )
            }
    }
}

data class PaymentMethodBreakdown(
    val method: String,
    val amount: Double,
    val count: Int
)

data class MonthlyArchiveSummary(
    val monthKey: String,
    val year: Int,
    val monthZeroBased: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val totalBudget: Double,
    val transactionCount: Int,
    val expenseCount: Int,
    val incomeCount: Int,
    val averageDailyExpense: Double,
    val topExpenseCategories: List<CategoryBreakdown>,
    val incomeBreakdown: List<CategoryBreakdown>,
    val paymentBreakdown: List<PaymentMethodBreakdown>,
    val budgetStatuses: List<BudgetStatus>,
    val transactions: List<FinanceTransaction>
)

data class DashboardTotals(
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val totalMonthlyBudget: Double = 0.0,
    val remainingMonthlyBudget: Double = 0.0
)
