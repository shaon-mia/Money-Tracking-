package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.FinanceTransaction
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatMoney
import com.example.ui.i18n.formatMonthYear
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransportBlue
import com.example.ui.viewmodel.FinanceDashboardState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onBack: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit
) {
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances

    val todayCal = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var currentMonthZero by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(todayCal.get(Calendar.DAY_OF_MONTH)) }

    val monthCal = remember(currentYear, currentMonthZero) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonthZero)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = (monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7

    val txByDay = remember(state.transactions, currentYear, currentMonthZero) {
        val map = mutableMapOf<Int, MutableList<FinanceTransaction>>()
        val cal = Calendar.getInstance()
        state.transactions.forEach { tx ->
            cal.timeInMillis = tx.timestampMillis
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonthZero) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(d) { mutableListOf() }.add(tx)
            }
        }
        map
    }

    val safeSelectedDay = selectedDay.coerceIn(1, daysInMonth)
    val dayTxs = txByDay[safeSelectedDay].orEmpty()
    val dayIncome = dayTxs.filter { it.type == "income" }.sumOf { it.amount }
    val dayExpense = dayTxs.filter { it.type == "expense" }.sumOf { it.amount }
    val dayBalance = dayIncome - dayExpense
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val dayHeaders = if (lang == "bn") listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি")
    else listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.calendar, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month navigation + Grid
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (currentMonthZero == 0) {
                                    currentMonthZero = 11
                                    currentYear -= 1
                                } else currentMonthZero -= 1
                                selectedDay = 1
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Month")
                            }
                            Text(
                                text = formatMonthYear(currentYear, currentMonthZero, lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                if (currentMonthZero == 11) {
                                    currentMonthZero = 0
                                    currentYear += 1
                                } else currentMonthZero += 1
                                selectedDay = 1
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Month")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            dayHeaders.forEach { dh ->
                                Text(
                                    text = dh,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))

                        val totalCells = firstDayOffset + daysInMonth
                        val rows = (totalCells + 6) / 7
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (r in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (c in 0 until 7) {
                                        val cellIdx = r * 7 + c
                                        val dayNum = cellIdx - firstDayOffset + 1
                                        if (dayNum in 1..daysInMonth) {
                                            val isSelected = dayNum == safeSelectedDay
                                            val list = txByDay[dayNum].orEmpty()
                                            val hasInc = list.any { it.type == "income" }
                                            val hasExp = list.any { it.type == "expense" }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent
                                                    )
                                                    .border(
                                                        width = if (isSelected) 0.dp else 0.5.dp,
                                                        color = if (list.isNotEmpty() && !isSelected)
                                                            MaterialTheme.colorScheme.outline else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedDay = dayNum },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (hasInc || hasExp) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            if (hasInc) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isSelected) Color.White else IncomeGreen)
                                                                )
                                                            }
                                                            if (hasExp) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isSelected) Color.White else ExpenseRed)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Day summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayMetricCard(strings.income, formatMoney(dayIncome, currency, hide), IncomeGreen, Modifier.weight(1f))
                    DayMetricCard(strings.expense, formatMoney(dayExpense, currency, hide), ExpenseRed, Modifier.weight(1f))
                    DayMetricCard(strings.dailyBalance, formatMoney(dayBalance, currency, hide), TransportBlue, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    text = "${strings.dayTransactions} ($safeSelectedDay ${formatMonthYear(currentYear, currentMonthZero, lang)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (dayTxs.isEmpty()) {
                item { EmptyStateCard(title = strings.noTransactions) }
            } else {
                items(dayTxs, key = { it.id }) { tx ->
                    val cat = catMap[tx.categoryId]
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
            }
        }
    }
}

@Composable
private fun DayMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
