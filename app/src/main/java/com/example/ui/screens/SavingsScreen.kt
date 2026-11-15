package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.data.model.SavingsGoal
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.DemoBadgeChip
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.CurrencyCatalog
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatMoney
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceDashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onBack: () -> Unit,
    onSaveMonthlyGoal: (Double) -> Unit,
    onSaveGoal: (SavingsGoal) -> Unit,
    onAddContribution: (SavingsGoal, Double, String) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances

    val totalSaved = state.savingsGoals.sumOf { it.savedAmount }
    var showNewGoalDialog by remember { mutableStateOf(false) }
    var showMonthlyGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.savings, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    Button(
                        onClick = { showNewGoalDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("add_savings_goal_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addSavingsGoal)
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
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.totalSavings, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatMoney(totalSaved, currency, hide),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            IconButton(onClick = { showMonthlyGoalDialog = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = strings.monthlySavingsTarget)
                            }
                        }
                        Text(
                            text = "${strings.monthlySavingsTarget}: ${formatMoney(state.preferences.monthlySavingsGoal, currency, hide)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.savingsGoals.isEmpty()) {
                item { EmptyStateCard(title = strings.addSavingsGoal) }
            } else {
                items(state.savingsGoals, key = { it.id }) { goal ->
                    val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
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
                                CategoryBadgeIcon(iconKey = goal.iconKey, colorHex = goal.colorHex, size = 42.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (goal.isDemo) DemoBadgeChip(strings.demoBadge)
                                    }
                                    Text(
                                        text = "${formatMoney(goal.savedAmount, currency, hide)} / ${formatMoney(goal.targetAmount, currency, hide)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                color = Color(goal.colorHex),
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
                                    text = "${(pct * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(goal.colorHex)
                                )
                                OutlinedButton(
                                    onClick = { contributingGoal = goal },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.addMoney)
                                }
                            }
                        }
                    }
                }
            }

            if (state.savingsEntries.isNotEmpty()) {
                item {
                    Text(
                        text = strings.savingsHistory,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.savingsEntries, key = { it.id }) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.goalTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (entry.note.isNotBlank()) {
                                    Text(entry.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = formatDateTime(entry.timestampMillis, lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "+${formatMoney(entry.amount, currency, hide)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewGoalDialog) {
        var title by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showNewGoalDialog = false },
            title = { Text(strings.addSavingsGoal, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        label = { Text(strings.goalTitle) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) targetText = it },
                        label = { Text("${strings.targetAmount} (${CurrencyCatalog.symbolFor(currency)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    if (err != null) Text(err ?: "", color = ExpenseRed)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = targetText.toDoubleOrNull()
                    if (title.isBlank() || amt == null || amt <= 0.0) {
                        err = strings.invalidInputErr
                        return@Button
                    }
                    onSaveGoal(
                        SavingsGoal(
                            title = title.trim(),
                            targetAmount = amt,
                            savedAmount = 0.0,
                            targetDateMillis = System.currentTimeMillis() + 90L * 86_400_000L
                        )
                    )
                    showNewGoalDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showNewGoalDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showMonthlyGoalDialog) {
        var amountText by remember {
            mutableStateOf(state.preferences.monthlySavingsGoal.toLong().toString())
        }
        AlertDialog(
            onDismissRequest = { showMonthlyGoalDialog = false },
            title = { Text(strings.monthlySavingsTarget, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = it },
                    label = { Text(CurrencyCatalog.symbolFor(currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt >= 0.0) {
                        onSaveMonthlyGoal(amt)
                        showMonthlyGoalDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showMonthlyGoalDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    contributingGoal?.let { goal ->
        var amountText by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { contributingGoal = null },
            title = { Text("${strings.addMoney} • ${goal.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = it },
                        label = { Text("${strings.amount} (${CurrencyCatalog.symbolFor(currency)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 200) note = it },
                        label = { Text(strings.note) },
                        singleLine = true
                    )
                    if (err != null) Text(err ?: "", color = ExpenseRed)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        err = strings.invalidAmountErr
                        return@Button
                    }
                    onAddContribution(goal, amt, note)
                    contributingGoal = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { contributingGoal = null }) { Text(strings.cancel) }
            }
        )
    }
}
