package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.rememberPressBounce
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatMoney
import com.example.ui.i18n.formatMonthYear
import com.example.ui.i18n.formatShortDate
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TransportBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceDashboardState
import com.example.util.FinancialCalculator
import com.example.util.MonthlyArchiveSummary
import com.example.util.ReportExporter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthlyArchiveScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onBack: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddExpenseForMonth: (Long) -> Unit,
    initialMonthKey: String? = null
) {
    val context = LocalContext.current
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances
    val currentMonthKey = remember { DefaultCatalog.currentMonthKey() }

    val archives = remember(state.transactions, state.categories, state.budgets) {
        FinancialCalculator.computeMonthlyArchives(
            transactions = state.transactions,
            categories = state.categories,
            budgets = state.budgets
        )
    }

    val availableYears = remember(archives) {
        archives.map { it.year }.distinct().sortedDescending()
    }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonthKey by remember(initialMonthKey) { mutableStateOf(initialMonthKey) }

    if (selectedMonthKey != null) {
        BackHandler {
            selectedMonthKey = null
        }
    }

    val filteredArchives = remember(archives, selectedYear) {
        if (selectedYear == null) archives else archives.filter { it.year == selectedYear }
    }

    val activeDetail = remember(selectedMonthKey, state.transactions, state.categories, state.budgets) {
        selectedMonthKey?.let { mk ->
            archives.firstOrNull { it.monthKey == mk } ?: FinancialCalculator.buildMonthSummary(
                monthKey = mk,
                transactions = state.transactions,
                categories = state.categories,
                budgets = state.budgets
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    val detail = activeDetail
                    Text(
                        text = if (detail != null) {
                            formatMonthYear(detail.year, detail.monthZeroBased, lang)
                        } else {
                            strings.monthlyRecords
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedMonthKey != null) {
                                selectedMonthKey = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.cancel
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeDetail?.monthKey,
            transitionSpec = {
                val enteringDetail = targetState != null && initialState == null
                val returningToList = targetState == null && initialState != null
                when {
                    enteringDetail -> (fadeIn(tween(360, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it / 8 })
                        .togetherWith(fadeOut(tween(260)) + slideOutHorizontally(tween(280)) { -it / 10 })
                    returningToList -> (fadeIn(tween(360, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { -it / 8 })
                        .togetherWith(fadeOut(tween(260)) + slideOutHorizontally(tween(280)) { it / 8 })
                    else -> {
                        val forward = (targetState ?: "") > (initialState ?: "")
                        (fadeIn(tween(320, easing = FastOutSlowInEasing)) + slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { if (forward) it / 8 else -it / 8 })
                            .togetherWith(fadeOut(tween(220)) + slideOutHorizontally(tween(240)) { if (forward) -it / 8 else it / 8 })
                    }
                }
            },
            modifier = Modifier.padding(innerPadding),
            label = "monthly_archive_screen_anim"
        ) { targetMonthKey ->
        val detail = remember(targetMonthKey, state.transactions, state.categories, state.budgets) {
            targetMonthKey?.let { mk ->
                archives.firstOrNull { it.monthKey == mk } ?: FinancialCalculator.buildMonthSummary(
                    monthKey = mk,
                    transactions = state.transactions,
                    categories = state.categories,
                    budgets = state.budgets
                )
            }
        }
        if (detail == null) {
            // Mode 1: List of all saved months
            val totalSavedIncome = remember(filteredArchives) { filteredArchives.sumOf { it.totalIncome } }
            val totalSavedExpense = remember(filteredArchives) { filteredArchives.sumOf { it.totalExpense } }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AnimatedEntry(index = 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${filteredArchives.size} ${strings.savedMonthsCountSuffix}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = strings.monthlyRecordsSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedMoneyText(
                                    amount = totalSavedIncome,
                                    currencyCode = currency,
                                    hideBalances = hide,
                                    prefix = "${strings.income}: ",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                                AnimatedMoneyText(
                                    amount = totalSavedExpense,
                                    currencyCode = currency,
                                    hideBalances = hide,
                                    prefix = "${strings.expense}: ",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                    }
                }

                if (availableYears.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedYear == null,
                                    onClick = { selectedYear = null },
                                    label = { Text(strings.allYears) }
                                )
                            }
                            items(availableYears, key = { it }) { yr ->
                                FilterChip(
                                    selected = selectedYear == yr,
                                    onClick = { selectedYear = yr },
                                    label = { Text("$yr") }
                                )
                            }
                        }
                    }
                }

                items(filteredArchives, key = { it.monthKey }) { summary ->
                    MonthSummaryCard(
                        summary = summary,
                        isCurrentMonth = summary.monthKey == currentMonthKey,
                        currencyCode = currency,
                        hideBalances = hide,
                        language = lang,
                        strings = strings,
                        modifier = Modifier.animateItem(),
                        onClick = { selectedMonthKey = summary.monthKey }
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        } else {
            // Mode 2: Complete Details of Selected Month
            MonthDetailContent(
                detail = detail,
                state = state,
                strings = strings,
                isCurrentMonth = detail.monthKey == currentMonthKey,
                onSelectMonthKey = { selectedMonthKey = it },
                onEditTransaction = onEditTransaction,
                onDeleteTransaction = onDeleteTransaction,
                onAddExpenseForMonth = {
                    val ts = FinancialCalculator.defaultTimestampForMonth(detail.monthKey)
                    onAddExpenseForMonth(ts)
                },
                onExportPdf = {
                    runCatching {
                        val heading = formatMonthYear(detail.year, detail.monthZeroBased, lang)
                        val file = ReportExporter.writePdfReportToFile(
                            context = context,
                            title = strings.monthlyRecords,
                            periodLabel = heading,
                            totalIncome = detail.totalIncome,
                            totalExpense = detail.totalExpense,
                            netSavings = detail.netBalance,
                            transactions = detail.transactions,
                            currencyCode = currency,
                            language = lang
                        )
                        ReportExporter.shareFile(context, file, "application/pdf", strings.exportPdf)
                    }.onFailure {
                        Toast.makeText(context, it.localizedMessage ?: "Export error", Toast.LENGTH_SHORT).show()
                    }
                },
                onExportCsv = {
                    runCatching {
                        val csv = ReportExporter.buildCsvContent(detail.transactions, currency, lang)
                        val file = ReportExporter.writeTextFileToCache(
                            context,
                            "MoneyTracker_${detail.monthKey}.csv",
                            csv
                        )
                        ReportExporter.shareFile(context, file, "text/csv", strings.exportCsv)
                    }.onFailure {
                        Toast.makeText(context, it.localizedMessage ?: "Export error", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthSummaryCard(
    summary: MonthlyArchiveSummary,
    isCurrentMonth: Boolean,
    currencyCode: String,
    hideBalances: Boolean,
    language: String,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val monthTitle = formatMonthYear(summary.year, summary.monthZeroBased, language)
    val (interactionSource, bounceModifier) = rememberPressBounce(pressedScale = 0.97f)
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .then(bounceModifier)
            .fillMaxWidth()
            .testTag("month_card_${summary.monthKey}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = if (isCurrentMonth) OrangePrimary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isCurrentMonth) strings.currentMonthBadge else strings.savedBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCurrentMonth) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "${summary.transactionCount} ${strings.recordsCountSuffix}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverviewDotMetric(
                    dotColor = IncomeGreen,
                    label = strings.income,
                    value = formatMoney(summary.totalIncome, currencyCode, hideBalances),
                    modifier = Modifier.weight(1f)
                )
                OverviewDotMetric(
                    dotColor = ExpenseRed,
                    label = strings.expense,
                    value = formatMoney(summary.totalExpense, currencyCode, hideBalances),
                    modifier = Modifier.weight(1f)
                )
                OverviewDotMetric(
                    dotColor = MaterialTheme.colorScheme.onSurface,
                    label = strings.balanceLabel,
                    value = formatMoney(summary.netBalance, currencyCode, hideBalances),
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.totalBudget > 0.0) {
                val ratio = (summary.totalExpense / summary.totalBudget).toFloat().coerceIn(0f, 1f)
                val barColor = when {
                    summary.totalExpense > summary.totalBudget -> ExpenseRed
                    ratio >= 0.8f -> WarningAmber
                    else -> IncomeGreen
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.totalBudget,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatMoney(summary.totalExpense, currencyCode, hideBalances)} / ${formatMoney(summary.totalBudget, currencyCode, hideBalances)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val animatedBudgetRatio by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                        label = "month_card_budget_${summary.monthKey}"
                    )
                    LinearProgressIndicator(
                        progress = { animatedBudgetRatio },
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                    )
                }
            }

            if (summary.topExpenseCategories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    summary.topExpenseCategories.take(3).forEach { cat ->
                        Surface(
                            color = Color(cat.colorHex).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex))
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "${cat.displayName(language)}: ${formatMoney(cat.amount, currencyCode, hideBalances)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.openMonthDetails,
                    style = MaterialTheme.typography.labelLarge,
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthDetailContent(
    detail: MonthlyArchiveSummary,
    state: FinanceDashboardState,
    strings: AppStrings,
    isCurrentMonth: Boolean,
    onSelectMonthKey: (String) -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onAddExpenseForMonth: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    var activeChartTab by remember { mutableIntStateOf(0) } // 0 = Expense, 1 = Income
    var txTypeFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTxs = remember(detail.transactions, txTypeFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        detail.transactions.filter { tx ->
            val matchType = txTypeFilter == "all" || tx.type == txTypeFilter
            val matchQ = q.isEmpty() ||
                tx.note.lowercase().contains(q) ||
                tx.categoryNameBn.lowercase().contains(q) ||
                tx.categoryNameEn.lowercase().contains(q)
            matchType && matchQ
        }
    }
    val groupedByDate = remember(filteredTxs, lang) {
        filteredTxs.groupBy { formatShortDate(it.timestampMillis, lang) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Switcher (< আগের মাস | নভেম্বর ২০২৬ | পরের মাস >)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onSelectMonthKey(FinancialCalculator.adjacentMonthKey(detail.monthKey, -1)) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_month_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMonthYear(detail.year, detail.monthZeroBased, lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCurrentMonth) strings.currentMonthBadge else strings.savedBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentMonth) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    onClick = { onSelectMonthKey(FinancialCalculator.adjacentMonthKey(detail.monthKey, +1)) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_month_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }
            }
        }

        // Complete Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OverviewDotMetric(
                            dotColor = IncomeGreen,
                            label = "${strings.income} (${detail.incomeCount})",
                            amount = detail.totalIncome,
                            currencyCode = currency,
                            hideBalances = hide,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewDotMetric(
                            dotColor = ExpenseRed,
                            label = "${strings.expense} (${detail.expenseCount})",
                            amount = detail.totalExpense,
                            currencyCode = currency,
                            hideBalances = hide,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewDotMetric(
                            dotColor = MaterialTheme.colorScheme.onSurface,
                            label = strings.balanceLabel,
                            amount = detail.netBalance,
                            currencyCode = currency,
                            hideBalances = hide,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.avgDailyExpense,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatMoney(detail.averageDailyExpense, currency, hide),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TransportBlue
                            )
                        }
                        if (detail.totalBudget > 0.0) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = strings.remainingBudget,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatMoney((detail.totalBudget - detail.totalExpense).coerceAtLeast(0.0), currency, hide),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Month Actions: Add Expense Here + Export PDF + Export CSV
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddExpenseForMonth,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_expense_in_month_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.addExpenseThisMonth, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportPdf, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportCsv, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Category Breakdown Donut Card
        if (detail.topExpenseCategories.isNotEmpty() || detail.incomeBreakdown.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.animateContentSize(animationSpec = tween(340, easing = FastOutSlowInEasing))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(0 to strings.expense, 1 to strings.income).forEach { (idx, title) ->
                                val selected = activeChartTab == idx
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeChartTab = idx },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.5.dp)
                                            .background(if (selected) OrangePrimary else Color.Transparent)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        val chartItems = if (activeChartTab == 0) detail.topExpenseCategories else detail.incomeBreakdown
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            AnimatedContent(
                                targetState = activeChartTab,
                                transitionSpec = {
                                    (fadeIn(tween(320, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = FastOutSlowInEasing)))
                                        .togetherWith(fadeOut(tween(220)))
                                },
                                label = "archive_chart_tab_anim"
                            ) { tab ->
                                val tabItems = if (tab == 0) detail.topExpenseCategories else detail.incomeBreakdown
                                if (tabItems.isEmpty()) {
                                    EmptyStateCard(title = strings.noMonthData)
                                } else {
                                    val monthWord = formatMonthYear(detail.year, detail.monthZeroBased, lang)
                                    CategoryDonutChart(
                                        items = tabItems,
                                        currencyCode = currency,
                                        hideBalances = hide,
                                        language = lang,
                                        centerSubtitle = monthWord
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Payment Method Summary Card
        if (detail.paymentBreakdown.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.paymentSummaryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val expTotal = detail.totalExpense.coerceAtLeast(1.0)
                        detail.paymentBreakdown.forEach { pm ->
                            val methodLabel = when (pm.method) {
                                "bank" -> strings.bank
                                "mobile_banking" -> strings.mobileBanking
                                "other" -> strings.otherMethod
                                else -> strings.cash
                            }
                            val pct = (pm.amount / expTotal).toFloat().coerceIn(0f, 1f)
                            val animPct by animateFloatAsState(
                                targetValue = pct,
                                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing),
                                label = "pm_bar_${pm.method}"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$methodLabel (${pm.count})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatMoney(pm.amount, currency, hide),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { animPct },
                                    color = OrangePrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Budgets Status Card
        if (detail.budgetStatuses.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.monthlyBudgetStatus,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        detail.budgetStatuses.forEach { bs ->
                            val ratio = bs.progress.coerceIn(0f, 1f)
                            val color = when {
                                bs.isExceeded -> ExpenseRed
                                bs.isWarning80 -> WarningAmber
                                else -> IncomeGreen
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bs.budget.categoryDisplayName(lang),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${formatMoney(bs.spent, currency, hide)} / ${formatMoney(bs.budget.limitAmount, currency, hide)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // All Transactions This Month Header + Search + Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${strings.allMonthTransactions} (${detail.transactionCount})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (detail.transactions.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchHint) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = txTypeFilter == "all",
                            onClick = { txTypeFilter = "all" },
                            label = { Text(strings.all) }
                        )
                        FilterChip(
                            selected = txTypeFilter == "expense",
                            onClick = { txTypeFilter = "expense" },
                            label = { Text(strings.expense) }
                        )
                        FilterChip(
                            selected = txTypeFilter == "income",
                            onClick = { txTypeFilter = "income" },
                            label = { Text(strings.income) }
                        )
                    }
                }
            }
        }

        if (filteredTxs.isEmpty()) {
            item {
                EmptyStateCard(title = strings.noMonthData)
            }
        } else {
            groupedByDate.forEach { (dateLabel, dayTxs) ->
                val dayInc = dayTxs.filter { it.type == "income" }.sumOf { it.amount }
                val dayExp = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
                item(key = "archive_header_$dateLabel") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (dayInc > 0.0) {
                                Text(
                                    text = "+${formatMoney(dayInc, currency, hide)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IncomeGreen
                                )
                            }
                            if (dayExp > 0.0) {
                                Text(
                                    text = "-${formatMoney(dayExp, currency, hide)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }

                items(dayTxs, key = { "archive_${it.id}" }) { tx ->
                    val cat = catMap[tx.categoryId]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TransactionRowCard(
                                tx = tx,
                                iconKey = cat?.iconKey ?: if (tx.type == "income") "payments" else "category",
                                colorHex = cat?.colorHex ?: if (tx.type == "income") 0xFF10B981L else 0xFFFF6B2CL,
                                currencyCode = currency,
                                hideBalances = hide,
                                language = lang,
                                strings = strings,
                                onClick = { onEditTransaction(tx) }
                            )
                        }
                        IconButton(
                            onClick = { onDeleteTransaction(tx.id) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}
