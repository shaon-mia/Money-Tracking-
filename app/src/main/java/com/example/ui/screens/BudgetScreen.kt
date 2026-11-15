package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetItem
import com.example.data.model.CategoryItem
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.DemoBadgeChip
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.CurrencyCatalog
import com.example.ui.i18n.formatMoney
import com.example.ui.i18n.formatMonthYear
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceDashboardState
import com.example.util.FinancialCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onSaveBudget: (BudgetItem) -> Unit,
    onDeleteBudget: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances

    val initialCal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonthZero by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    val monthKey = remember(selectedYear, selectedMonthZero) {
        String.format("%04d-%02d", selectedYear, selectedMonthZero + 1)
    }

    val expenseCategories = remember(state.categories) { state.categories.filter { it.type == "expense" } }
    val catMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val budgetStatuses = remember(state.budgets, state.transactions, monthKey) {
        FinancialCalculator.computeBudgetStatuses(state.budgets, state.transactions, monthKey)
    }

    val totalLimit = budgetStatuses.sumOf { it.budget.limitAmount }
    val totalSpent = budgetStatuses.sumOf { it.spent }
    val totalRemaining = (totalLimit - totalSpent).coerceAtLeast(0.0)
    val overallProgress = if (totalLimit > 0.0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f

    var editingBudget by remember { mutableStateOf<BudgetItem?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.monthlyBudgetTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        editingBudget = null
                        showBudgetDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_budget_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.setBudget)
                }
            }
        }

        // Month selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonthZero == 0) {
                            selectedMonthZero = 11
                            selectedYear -= 1
                        } else {
                            selectedMonthZero -= 1
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    Text(
                        text = formatMonthYear(selectedYear, selectedMonthZero, lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        if (selectedMonthZero == 11) {
                            selectedMonthZero = 0
                            selectedYear += 1
                        } else {
                            selectedMonthZero += 1
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Overall monthly summary card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.totalBudget, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatMoney(totalLimit, currency, hide), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(strings.remaining, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatMoney(totalRemaining, currency, hide),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (totalSpent > totalLimit && totalLimit > 0) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { overallProgress },
                        color = if (totalSpent > totalLimit && totalLimit > 0) ExpenseRed else OrangePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = "${strings.spent}: ${formatMoney(totalSpent, currency, hide)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (budgetStatuses.isEmpty()) {
            item {
                EmptyStateCard(title = strings.setBudget)
            }
        } else {
            items(budgetStatuses, key = { it.budget.id }) { status ->
                val cat = catMap[status.budget.categoryId]
                val barColor = when {
                    status.isExceeded -> ExpenseRed
                    status.isWarning80 -> WarningAmber
                    else -> IncomeGreen
                }
                val animatedProgress by animateFloatAsState(
                    targetValue = status.progress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
                    label = "budget_progress"
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadgeIcon(
                                iconKey = cat?.iconKey ?: "category",
                                colorHex = cat?.colorHex ?: 0xFFFF6B2CL,
                                size = 42.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = status.budget.categoryDisplayName(lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (status.budget.isDemo) {
                                        DemoBadgeChip(strings.demoBadge)
                                    }
                                }
                                Text(
                                    text = "${formatMoney(status.spent, currency, hide)} / ${formatMoney(status.budget.limitAmount, currency, hide)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                editingBudget = status.budget
                                showBudgetDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.setBudget)
                            }
                            IconButton(onClick = { onDeleteBudget(status.budget.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(status.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = barColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${strings.remaining}: ${formatMoney(status.remaining, currency, hide)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (status.remaining < 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (status.isExceeded || status.isWarning80) {
                            Surface(
                                color = barColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = barColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (status.isExceeded) strings.budgetExceeded else strings.budgetWarn80,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = barColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showBudgetDialog) {
        BudgetEditorDialog(
            existing = editingBudget,
            expenseCategories = expenseCategories,
            monthKey = monthKey,
            currencyCode = currency,
            language = lang,
            strings = strings,
            onDismiss = { showBudgetDialog = false },
            onSave = { budget ->
                onSaveBudget(budget)
                showBudgetDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditorDialog(
    existing: BudgetItem?,
    expenseCategories: List<CategoryItem>,
    monthKey: String,
    currencyCode: String,
    language: String,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (BudgetItem) -> Unit
) {
    var selectedCategory by remember(existing) {
        mutableStateOf(
            expenseCategories.firstOrNull { it.id == existing?.categoryId }
                ?: expenseCategories.firstOrNull()
        )
    }
    var limitText by remember(existing) {
        mutableStateOf(
            existing?.limitAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.setBudget, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.displayName(language) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName(language)) },
                                onClick = {
                                    selectedCategory = cat
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) {
                            limitText = input
                            errorText = null
                        }
                    },
                    label = { Text("${strings.budgetLimit} (${CurrencyCatalog.symbolFor(currencyCode)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(errorText ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = limitText.toDoubleOrNull()
                val cat = selectedCategory
                if (limit == null || limit <= 0.0 || cat == null) {
                    errorText = strings.invalidAmountErr
                    return@Button
                }
                onSave(
                    BudgetItem(
                        id = existing?.id ?: "",
                        userId = existing?.userId ?: "",
                        categoryId = cat.id,
                        categoryNameBn = cat.nameBn,
                        categoryNameEn = cat.nameEn,
                        monthKey = monthKey,
                        limitAmount = limit,
                        isDemo = false,
                        createdAt = existing?.createdAt
                    )
                )
            }) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
