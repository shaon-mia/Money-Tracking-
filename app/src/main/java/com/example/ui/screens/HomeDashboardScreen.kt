package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultCatalog
import com.example.data.model.FinanceTransaction
import com.example.ui.components.AnimatedEntry
import com.example.ui.components.AnimatedMoneyText
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.DemoBadgeChip
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatMoney
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TransportBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceDashboardState
import com.example.util.FinancialCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onToggleHideBalance: () -> Unit,
    onClearDemoData: () -> Unit,
    onViewExpenses: () -> Unit,
    onOpenLendBorrow: () -> Unit,
    onOpenMonthlyArchive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val prefs = state.preferences
    val currency = prefs.currencyCode
    val hide = prefs.hideBalances
    val lang = prefs.language
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var selectedDayMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var activeDonutTab by remember { mutableIntStateOf(0) } // 0 = Expenses, 1 = Income, 2 = Loans

    val selectedMonthKey = remember(selectedDayMillis) {
        DefaultCatalog.monthKeyOf(selectedDayMillis)
    }
    val totals = remember(state.transactions, state.budgets, selectedDayMillis) {
        FinancialCalculator.computeDashboardTotals(state.transactions, state.budgets, selectedDayMillis)
    }
    val monthTxs = remember(state.transactions, selectedMonthKey) {
        state.transactions.filter { DefaultCatalog.monthKeyOf(it.timestampMillis) == selectedMonthKey }
    }
    val monthExpenseBreakdown = remember(monthTxs, state.categories) {
        FinancialCalculator.computeCategoryBreakdown(monthTxs, state.categories, "expense")
    }
    val monthIncomeBreakdown = remember(monthTxs, state.categories) {
        FinancialCalculator.computeCategoryBreakdown(monthTxs, state.categories, "income")
    }
    val budgetAlerts = remember(state.budgets, state.transactions, selectedMonthKey) {
        FinancialCalculator.computeBudgetStatuses(state.budgets, state.transactions, selectedMonthKey)
            .filter { it.isWarning80 || it.isExceeded }
    }

    val totalToReceive = remember(state.lendBorrows) {
        state.lendBorrows.filter { it.kind == "lend" }.sumOf { it.remainingAmount }
    }
    val totalToRepay = remember(state.lendBorrows) {
        state.lendBorrows.filter { it.kind == "borrow" }.sumOf { it.remainingAmount }
    }

    val monthNameOnly = remember(selectedDayMillis, lang) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDayMillis }
        val m = cal.get(Calendar.MONTH)
        val bnMonths = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val enMonths = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        if (lang == "bn") bnMonths.getOrElse(m) { "" } else enMonths.getOrElse(m) { "" }
    }

    val dateHeaderLabel = remember(selectedDayMillis, lang, strings.todayWord) {
        val locale = if (lang == "bn") Locale("bn", "BD") else Locale.ENGLISH
        val formatted = SimpleDateFormat("EEE, d MMM", locale).format(Date(selectedDayMillis))
        if (FinancialCalculator.isSameDay(selectedDayMillis, System.currentTimeMillis())) {
            "${strings.todayWord} • $formatted"
        } else {
            formatted
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Pinned Date Header at the top of Home Page
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_pinned_date_bar")
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedDayMillis
                                add(Calendar.DAY_OF_YEAR, -1)
                            }
                            selectedDayMillis = cal.timeInMillis
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Previous day",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    AnimatedContent(
                        targetState = dateHeaderLabel,
                        transitionSpec = {
                            (fadeIn(tween(280, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 6 })
                                .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(220)) { -it / 6 })
                        },
                        label = "home_date_header_anim"
                    ) { labelText ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedDayMillis = System.currentTimeMillis() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = selectedDayMillis
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            selectedDayMillis = cal.timeInMillis
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Next day",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

        // 2. CoinPie Monthly Overview Pill Card (Income • Expense • Balance)
        item {
            AnimatedEntry(index = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$monthNameOnly ${strings.overviewSuffix}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onToggleHideBalance)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("toggle_hide_balance")
                    ) {
                        Text(
                            text = "${strings.currentBalance}: ${formatMoney(totals.currentBalance, currency, hide)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = strings.hideBalancesLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onOpenMonthlyArchive() }
                        .testTag("home_monthly_overview_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OverviewDotMetric(
                            dotColor = IncomeGreen,
                            label = strings.income,
                            amount = totals.monthlyIncome,
                            currencyCode = currency,
                            hideBalances = hide,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewDotMetric(
                            dotColor = OrangePrimary,
                            label = strings.expense,
                            amount = totals.monthlyExpense,
                            currencyCode = currency,
                            hideBalances = hide,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewDotMetric(
                            dotColor = MaterialTheme.colorScheme.onSurface,
                            label = strings.balanceLabel,
                            amount = totals.monthlySavings,
                            currencyCode = currency,
                            hideBalances = hide,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            }
        }

        // Demo Banner if demo records exist
        item {
            AnimatedVisibility(
                visible = state.hasDemoData,
                enter = fadeIn(tween(340, easing = FastOutSlowInEasing)) + expandVertically(tween(340, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(240)) + shrinkVertically(tween(240))
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.demoBannerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = strings.demoBannerSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        TextButton(
                            onClick = onClearDemoData,
                            modifier = Modifier.testTag("clear_demo_button")
                        ) {
                            Text(
                                text = strings.clearDemo,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 3. CoinPie Main Donut Breakdown Card (Expenses | Income | Loans tabs)
        item {
            AnimatedEntry(index = 1) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.animateContentSize(animationSpec = tween(340, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            0 to strings.expense,
                            1 to strings.income,
                            2 to strings.loansLabel
                        ).forEach { (tabIdx, title) ->
                            val selected = activeDonutTab == tabIdx
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeDonutTab = tabIdx }
                                    .padding(top = 10.dp, bottom = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) OrangePrimary else Color.Transparent)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        AnimatedContent(
                            targetState = activeDonutTab,
                            transitionSpec = {
                                (fadeIn(tween(320, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = FastOutSlowInEasing)))
                                    .togetherWith(fadeOut(tween(220)))
                            },
                            label = "home_donut_tab_anim"
                        ) { tab ->
                        when (tab) {
                            0 -> {
                                if (monthExpenseBreakdown.isEmpty()) {
                                    EmptyStateCard(title = strings.noTransactions)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CategoryDonutChart(
                                            items = monthExpenseBreakdown,
                                            currencyCode = currency,
                                            hideBalances = hide,
                                            language = lang,
                                            centerSubtitle = "${strings.spentInPrefix} $monthNameOnly",
                                            onCategoryClick = { onViewExpenses() },
                                            compact = true
                                        )
                                        OutlinedButton(
                                            onClick = onViewExpenses,
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .testTag("home_view_expenses_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                                contentDescription = null,
                                                tint = OrangePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = strings.viewExpenses,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = OrangePrimary
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                if (monthIncomeBreakdown.isEmpty()) {
                                    EmptyStateCard(title = strings.noTransactions)
                                } else {
                                    CategoryDonutChart(
                                        items = monthIncomeBreakdown,
                                        currencyCode = currency,
                                        hideBalances = hide,
                                        language = lang,
                                        centerSubtitle = "${strings.incomeInPrefix} $monthNameOnly",
                                        compact = true
                                    )
                                }
                            }
                            else -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = IncomeGreen.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(11.dp)) {
                                                Text(strings.toReceive, style = MaterialTheme.typography.labelMedium, color = IncomeGreen)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = formatMoney(totalToReceive, currency, hide),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = IncomeGreen
                                                )
                                            }
                                        }
                                        Surface(
                                            color = ExpenseRed.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(11.dp)) {
                                                Text(strings.toRepay, style = MaterialTheme.typography.labelMedium, color = ExpenseRed)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = formatMoney(totalToRepay, currency, hide),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ExpenseRed
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = onOpenLendBorrow,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        Icon(Icons.Rounded.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.lendBorrow, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
            }
        }

        // 4. Today's & Budget Snapshot 2x2 Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricSummaryCard(
                        title = strings.todayIncome,
                        value = formatMoney(totals.todayIncome, currency, hide),
                        accent = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryCard(
                        title = strings.todayExpense,
                        value = formatMoney(totals.todayExpense, currency, hide),
                        accent = ExpenseRed,
                        onClick = onViewExpenses,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricSummaryCard(
                        title = strings.monthlySavings,
                        value = formatMoney(totals.monthlySavings, currency, hide),
                        accent = TransportBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricSummaryCard(
                        title = strings.remainingBudget,
                        value = formatMoney(totals.remainingMonthlyBudget, currency, hide),
                        accent = OrangePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Budget Alert Banner (80% / 100%)
        if (budgetAlerts.isNotEmpty()) {
            item {
                val topAlert = budgetAlerts.first()
                val alertColor = if (topAlert.isExceeded) ExpenseRed else WarningAmber
                Card(
                    colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = alertColor
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (topAlert.isExceeded) strings.budgetExceeded else strings.budgetWarn80,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = alertColor
                            )
                            Text(
                                text = "${topAlert.budget.categoryDisplayName(lang)} • ${(topAlert.progress * 100).toInt()}% (${formatMoney(topAlert.spent, currency, hide)} / ${formatMoney(topAlert.budget.limitAmount, currency, hide)})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
        }
    }
}

@Composable
fun OverviewDotMetric(
    dotColor: Color,
    label: String,
    value: String? = null,
    amount: Double? = null,
    currencyCode: String = "BDT",
    hideBalances: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (amount != null) {
            AnimatedMoneyText(
                amount = amount,
                currencyCode = currencyCode,
                hideBalances = hideBalances,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = value ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: FinanceTransaction,
    iconKey: String,
    colorHex: Long,
    currencyCode: String,
    hideBalances: Boolean,
    language: String,
    strings: AppStrings,
    onClick: () -> Unit
) {
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign = if (isIncome) "+" else "-"
    val methodLabel = when (tx.paymentMethod) {
        "bank" -> strings.bank
        "mobile_banking" -> strings.mobileBanking
        "other" -> strings.otherMethod
        else -> strings.cash
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryBadgeIcon(
                iconKey = iconKey,
                colorHex = colorHex,
                size = 44.dp,
                contentDescription = tx.categoryDisplayName(language)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tx.categoryDisplayName(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tx.isDemo) {
                        DemoBadgeChip(strings.demoBadge)
                    }
                }
                if (tx.note.isNotBlank()) {
                    Text(
                        text = tx.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${formatDateTime(tx.timestampMillis, language)} • $methodLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$sign${formatMoney(tx.amount, currencyCode, hideBalances)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
