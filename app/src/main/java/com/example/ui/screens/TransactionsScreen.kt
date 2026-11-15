package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultCatalog
import com.example.data.model.FinanceTransaction
import com.example.ui.components.AnimatedMoneyText
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatMoney
import com.example.ui.i18n.formatMonthYear
import com.example.ui.i18n.formatShortDate
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceDashboardState
import com.example.util.FinancialCalculator

@Composable
fun TransactionsScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMonthlyArchive: () -> Unit = {},
    initialTypeFilter: String = "all",
    filterTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(initialTypeFilter) }
    var methodFilter by remember { mutableStateOf("all") }
    var monthFilter by remember { mutableStateOf("all") }

    LaunchedEffect(initialTypeFilter, filterTrigger) {
        if (filterTrigger > 0) {
            typeFilter = initialTypeFilter
        }
    }

    val availableMonths = remember(state.transactions) {
        val keys = linkedSetOf<String>()
        keys.add(DefaultCatalog.currentMonthKey())
        state.transactions.forEach { keys.add(DefaultCatalog.monthKeyOf(it.timestampMillis)) }
        keys.sortedDescending()
    }

    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val filtered = remember(state.transactions, searchQuery, typeFilter, methodFilter, monthFilter, lang) {
        state.transactions.filter { tx ->
            val matchesMonth = monthFilter == "all" || DefaultCatalog.monthKeyOf(tx.timestampMillis) == monthFilter
            val matchesType = typeFilter == "all" || tx.type == typeFilter
            val matchesMethod = methodFilter == "all" || tx.paymentMethod == methodFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                tx.note.lowercase().contains(q) ||
                tx.categoryNameBn.lowercase().contains(q) ||
                tx.categoryNameEn.lowercase().contains(q)
            matchesMonth && matchesType && matchesMethod && matchesQuery
        }
    }

    val filteredTotal = remember(filtered) { filtered.sumOf { it.amount } }

    val groupedByDate = remember(filtered, lang) {
        filtered.groupBy { formatShortDate(it.timestampMillis, lang) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (typeFilter == "expense") strings.viewExpenses else strings.navTransactions,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenMonthlyArchive,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_monthly_archive_from_tx")
                    ) {
                        Text(strings.viewByMonth)
                    }
                    OutlinedButton(
                        onClick = onOpenCalendar,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_calendar_from_tx")
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = strings.calendar, modifier = Modifier.size(18.dp))
                    }
                }
            }

            AnimatedVisibility(
                visible = typeFilter == "expense" || typeFilter == "income",
                enter = fadeIn(tween(320)) + expandVertically(tween(320)),
                exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
            ) {
                val isExp = typeFilter == "expense"
                val accent = if (isExp) ExpenseRed else IncomeGreen
                Card(
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isExp) strings.totalFilteredExpense else strings.totalFilteredIncome,
                                style = MaterialTheme.typography.labelLarge,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${filtered.size} ${strings.recordsCountSuffix}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedMoneyText(
                            amount = filteredTotal,
                            currencyCode = currency,
                            hideBalances = hide,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_search_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = monthFilter == "all",
                        onClick = { monthFilter = "all" },
                        label = { Text(strings.allMonths) }
                    )
                }
                items(availableMonths, key = { "m_$it" }) { mk ->
                    val parsed = FinancialCalculator.parseMonthKey(mk)
                    val label = if (parsed != null) formatMonthYear(parsed.first, parsed.second, lang) else mk
                    FilterChip(
                        selected = monthFilter == mk,
                        onClick = { monthFilter = mk },
                        label = { Text(label) }
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = typeFilter == "all",
                        onClick = { typeFilter = "all" },
                        label = { Text(strings.all) }
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == "income",
                        onClick = { typeFilter = "income" },
                        label = { Text(strings.income) }
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == "expense",
                        onClick = { typeFilter = "expense" },
                        label = { Text(strings.expense) },
                        modifier = Modifier.testTag("filter_chip_expense")
                    )
                }
                item {
                    FilterChip(
                        selected = methodFilter == "cash",
                        onClick = { methodFilter = if (methodFilter == "cash") "all" else "cash" },
                        label = { Text(strings.cash) }
                    )
                }
                item {
                    FilterChip(
                        selected = methodFilter == "mobile_banking",
                        onClick = { methodFilter = if (methodFilter == "mobile_banking") "all" else "mobile_banking" },
                        label = { Text(strings.mobileBanking) }
                    )
                }
                item {
                    FilterChip(
                        selected = methodFilter == "bank",
                        onClick = { methodFilter = if (methodFilter == "bank") "all" else "bank" },
                        label = { Text(strings.bank) }
                    )
                }
                item {
                    FilterChip(
                        selected = methodFilter == "other",
                        onClick = { methodFilter = if (methodFilter == "other") "all" else "other" },
                        label = { Text(strings.otherMethod) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item { EmptyStateCard(title = strings.noTransactions) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (dateLabel, txs) ->
                    val dayInc = txs.filter { it.type == "income" }.sumOf { it.amount }
                    val dayExp = txs.filter { it.type == "expense" }.sumOf { it.amount }
                    item(key = "header_$dateLabel") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
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

                    items(txs, key = { it.id }) { tx ->
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
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
