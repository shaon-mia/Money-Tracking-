package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatMoney
import com.example.ui.i18n.formatMonthYear
import com.example.ui.i18n.formatShortDate
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.viewmodel.FinanceDashboardState
import com.example.util.FinancialCalculator
import com.example.util.ReportExporter
import java.util.Calendar

private enum class StatsPeriod { WEEKLY, MONTHLY, ANNUAL, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var period by remember { mutableStateOf(StatsPeriod.MONTHLY) }
    var periodOffset by remember { mutableIntStateOf(0) } // steps week / month / year
    var activeTypeTab by remember { mutableIntStateOf(0) } // 0 = Expenses, 1 = Income

    var customStart by remember { mutableLongStateOf(System.currentTimeMillis() - 30L * 86_400_000L) }
    var customEnd by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    val (rangeStart, rangeEnd, periodHeading) = remember(period, periodOffset, customStart, customEnd, lang) {
        val cal = Calendar.getInstance()
        when (period) {
            StatsPeriod.WEEKLY -> {
                cal.add(Calendar.WEEK_OF_YEAR, periodOffset)
                val endMs = FinancialCalculator.endOfDayMillis(cal.timeInMillis)
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val startMs = FinancialCalculator.startOfDayMillis(cal.timeInMillis)
                Triple(startMs, endMs, "${formatShortDate(startMs, lang)} – ${formatShortDate(endMs, lang)}")
            }
            StatsPeriod.MONTHLY -> {
                cal.add(Calendar.MONTH, periodOffset)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startMs = FinancialCalculator.startOfDayMillis(cal.timeInMillis)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                val endMs = FinancialCalculator.endOfDayMillis(cal.timeInMillis)
                val heading = formatMonthYear(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), lang)
                Triple(startMs, endMs, heading)
            }
            StatsPeriod.ANNUAL -> {
                cal.add(Calendar.YEAR, periodOffset)
                val yr = cal.get(Calendar.YEAR)
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val startMs = FinancialCalculator.startOfDayMillis(cal.timeInMillis)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                val endMs = FinancialCalculator.endOfDayMillis(cal.timeInMillis)
                Triple(startMs, endMs, "$yr")
            }
            StatsPeriod.CUSTOM -> {
                val startMs = FinancialCalculator.startOfDayMillis(customStart)
                val endMs = FinancialCalculator.endOfDayMillis(customEnd)
                Triple(startMs, endMs, "${formatShortDate(startMs, lang)} – ${formatShortDate(endMs, lang)}")
            }
        }
    }

    val filteredTxs = remember(state.transactions, rangeStart, rangeEnd) {
        state.transactions.filter { it.timestampMillis in rangeStart..rangeEnd }
    }
    val totalIncome = filteredTxs.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = filteredTxs.filter { it.type == "expense" }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense

    val chartDays = when (period) {
        StatsPeriod.WEEKLY -> 7
        StatsPeriod.MONTHLY -> 10
        StatsPeriod.ANNUAL -> 12
        StatsPeriod.CUSTOM -> 7
    }
    val dailySeries = remember(filteredTxs, chartDays, rangeEnd, lang) {
        val cappedEnd = minOf(rangeEnd, System.currentTimeMillis())
        FinancialCalculator.computeDailySeries(filteredTxs, days = chartDays, endMillis = cappedEnd, language = lang)
    }
    val expenseBreakdown = remember(filteredTxs, state.categories) {
        FinancialCalculator.computeCategoryBreakdown(filteredTxs, state.categories, "expense")
    }
    val incomeBreakdown = remember(filteredTxs, state.categories) {
        FinancialCalculator.computeCategoryBreakdown(filteredTxs, state.categories, "income")
    }

    val centerPeriodSub = when (period) {
        StatsPeriod.MONTHLY -> {
            val monthWord = periodHeading.substringBefore(" ").ifBlank { periodHeading }
            if (activeTypeTab == 0) "${strings.spentInPrefix} $monthWord" else "${strings.incomeInPrefix} $monthWord"
        }
        else -> if (activeTypeTab == 0) strings.expense else strings.income
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. CoinPie Pill Segmented Switcher (Weekly | Monthly | Annual | Custom)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activePillBg = if (isDark) OrangePrimary else CharcoalText
                    listOf(
                        StatsPeriod.WEEKLY to strings.weekly,
                        StatsPeriod.MONTHLY to strings.monthly,
                        StatsPeriod.ANNUAL to strings.annual,
                        StatsPeriod.CUSTOM to strings.customRange
                    ).forEach { (p, label) ->
                        val selected = period == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (selected) activePillBg else Color.Transparent)
                                .clickable {
                                    if (p == StatsPeriod.CUSTOM) {
                                        showDateRangeDialog = true
                                    } else {
                                        period = p
                                        periodOffset = 0
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. CoinPie Period Navigation Row (< November 2026 >)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        if (period == StatsPeriod.CUSTOM) showDateRangeDialog = true else periodOffset -= 1
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous period",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = periodHeading,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    onClick = {
                        if (period == StatsPeriod.CUSTOM) showDateRangeDialog = true else periodOffset += 1
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next period",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. CoinPie 3-Column Summary Pill Card (Income • Expense • Balance)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OverviewDotMetric(
                        dotColor = IncomeGreen,
                        label = strings.income,
                        value = formatMoney(totalIncome, currency, hide),
                        modifier = Modifier.weight(1f)
                    )
                    OverviewDotMetric(
                        dotColor = OrangePrimary,
                        label = strings.expense,
                        value = formatMoney(totalExpense, currency, hide),
                        modifier = Modifier.weight(1f)
                    )
                    OverviewDotMetric(
                        dotColor = MaterialTheme.colorScheme.onSurface,
                        label = strings.balanceLabel,
                        value = formatMoney(netSavings, currency, hide),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. CoinPie Expenses | Income Underline Tab + Donut Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.animateContentSize(animationSpec = tween(220))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(0 to strings.expense, 1 to strings.income).forEach { (idx, title) ->
                            val selected = activeTypeTab == idx
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTypeTab = idx },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 14.dp)
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

                    val itemsToShow = if (activeTypeTab == 0) expenseBreakdown else incomeBreakdown
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                        if (itemsToShow.isEmpty()) {
                            EmptyStateCard(title = strings.noTransactions)
                        } else {
                            CategoryDonutChart(
                                items = itemsToShow,
                                currencyCode = currency,
                                hideBalances = hide,
                                language = lang,
                                centerSubtitle = centerPeriodSub
                            )
                        }
                    }
                }
            }
        }

        // 5. Smart Insights + Export PDF & CSV Action Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            runCatching {
                                val file = ReportExporter.writePdfReportToFile(
                                    context = context,
                                    title = strings.navStats,
                                    periodLabel = periodHeading,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    netSavings = netSavings,
                                    transactions = filteredTxs,
                                    currencyCode = currency,
                                    language = lang
                                )
                                ReportExporter.shareFile(context, file, "application/pdf", strings.exportPdf)
                            }.onFailure {
                                Toast.makeText(context, it.localizedMessage ?: "Export error", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportPdf, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val csv = ReportExporter.buildCsvContent(filteredTxs, currency, lang)
                                val file = ReportExporter.writeTextFileToCache(
                                    context,
                                    "MoneyTracker_Report_${System.currentTimeMillis()}.csv",
                                    csv
                                )
                                ReportExporter.shareFile(context, file, "text/csv", strings.exportCsv)
                            }.onFailure {
                                Toast.makeText(context, it.localizedMessage ?: "Export error", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.exportCsv, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenInsights,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.smartInsights, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 6. Income vs Expense Bar Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.incomeVsExpense,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    IncomeExpenseBarChart(
                        points = dailySeries,
                        currencyCode = currency,
                        hideBalances = hide,
                        incomeLabel = strings.income,
                        expenseLabel = strings.expense
                    )
                }
            }
        }

        // 7. Highest Spending Categories
        if (expenseBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = strings.highestCategories,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(expenseBreakdown.take(5), key = { it.categoryId }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadgeIcon(iconKey = item.iconKey, colorHex = item.colorHex, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.displayName(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${item.percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = formatMoney(item.amount, currency, hide),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showDateRangeDialog) {
        val rangeState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStart,
            initialSelectedEndDateMillis = customEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangeDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val s = rangeState.selectedStartDateMillis
                    val e = rangeState.selectedEndDateMillis
                    if (s != null && e != null) {
                        customStart = s
                        customEnd = e
                        period = StatsPeriod.CUSTOM
                    }
                    showDateRangeDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangeDialog = false }) { Text(strings.cancel) }
            }
        ) {
            DateRangePicker(state = rangeState, modifier = Modifier.height(420.dp))
        }
    }
}
