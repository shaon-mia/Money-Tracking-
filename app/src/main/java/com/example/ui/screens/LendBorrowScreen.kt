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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.LendBorrowRecord
import com.example.ui.components.DemoBadgeChip
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.CurrencyCatalog
import com.example.ui.i18n.formatDateTime
import com.example.ui.i18n.formatMoney
import com.example.ui.i18n.formatShortDate
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceDashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onBack: () -> Unit,
    onSaveRecord: (LendBorrowRecord) -> Unit,
    onAddPayment: (LendBorrowRecord, Double, String) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val lang = state.preferences.language
    val currency = state.preferences.currencyCode
    val hide = state.preferences.hideBalances

    var filterKind by remember { mutableStateOf("all") }
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentTarget by remember { mutableStateOf<LendBorrowRecord?>(null) }

    val totalToReceive = state.lendBorrows.filter { it.kind == "lend" }.sumOf { it.remainingAmount }
    val totalToRepay = state.lendBorrows.filter { it.kind == "borrow" }.sumOf { it.remainingAmount }

    val visibleRecords = remember(state.lendBorrows, filterKind) {
        state.lendBorrows.filter { filterKind == "all" || it.kind == filterKind }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.lendBorrow, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("add_lend_borrow_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addLendBorrow)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(strings.toReceive, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatMoney(totalToReceive, currency, hide),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(strings.toRepay, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatMoney(totalToRepay, currency, hide),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filterKind == "all", onClick = { filterKind = "all" }, label = { Text(strings.all) })
                    FilterChip(selected = filterKind == "lend", onClick = { filterKind = "lend" }, label = { Text(strings.toReceive) })
                    FilterChip(selected = filterKind == "borrow", onClick = { filterKind = "borrow" }, label = { Text(strings.toRepay) })
                }
            }

            if (visibleRecords.isEmpty()) {
                item { EmptyStateCard(title = strings.addLendBorrow) }
            } else {
                items(visibleRecords, key = { it.id }) { rec ->
                    val statusColor = when (rec.status) {
                        "paid" -> IncomeGreen
                        "partial" -> WarningAmber
                        else -> ExpenseRed
                    }
                    val statusLabel = when (rec.status) {
                        "paid" -> strings.paid
                        "partial" -> strings.partial
                        else -> strings.unpaid
                    }
                    val payments = state.lendBorrowPayments.filter { it.recordId == rec.id }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(rec.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (rec.isDemo) DemoBadgeChip(strings.demoBadge)
                                    }
                                    if (rec.phoneNumber.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(4.dp))
                                            Text(rec.phoneNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteRecord(rec.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete)
                                }
                            }

                            if (rec.description.isNotBlank()) {
                                Text(rec.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${strings.amount}: ${formatMoney(rec.totalAmount, currency, hide)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${strings.remaining}: ${formatMoney(rec.remainingAmount, currency, hide)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rec.kind == "lend") IncomeGreen else ExpenseRed
                                )
                            }

                            Text(
                                text = "${formatShortDate(rec.dateMillis, lang)}" +
                                    if (rec.dueDateMillis > 0L) " • ${strings.dueDate}: ${formatShortDate(rec.dueDateMillis, lang)}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (rec.status != "paid") {
                                OutlinedButton(
                                    onClick = { paymentTarget = rec },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.recordPayment)
                                }
                            }

                            if (payments.isNotEmpty()) {
                                Text(
                                    text = strings.paymentHistory,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                payments.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${formatDateTime(p.timestampMillis, lang)} ${if (p.note.isNotBlank()) "(${p.note})" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatMoney(p.amount, currency, hide),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var kind by remember { mutableStateOf("lend") }
        var personName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var dueDateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 14L * 86_400_000L) }
        var showDuePicker by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addLendBorrow, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = kind == "lend",
                            onClick = { kind = "lend" },
                            label = { Text(strings.toReceive) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = kind == "borrow",
                            onClick = { kind = "borrow" },
                            label = { Text(strings.toRepay) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { if (it.length <= 80) personName = it },
                        label = { Text(strings.personName) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 30) phone = it },
                        label = { Text(strings.phoneOptional) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = it },
                        label = { Text("${strings.amount} (${CurrencyCatalog.symbolFor(currency)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedButton(onClick = { showDuePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${strings.dueDate}: ${formatShortDate(dueDateMillis, lang)}")
                    }
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { if (it.length <= 300) desc = it },
                        label = { Text(strings.note) },
                        maxLines = 2
                    )
                    if (err != null) Text(err ?: "", color = ExpenseRed)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (personName.isBlank() || amt == null || amt <= 0.0) {
                        err = strings.invalidInputErr
                        return@Button
                    }
                    onSaveRecord(
                        LendBorrowRecord(
                            kind = kind,
                            personName = personName.trim(),
                            phoneNumber = phone.trim(),
                            totalAmount = amt,
                            paidAmount = 0.0,
                            dateMillis = System.currentTimeMillis(),
                            dueDateMillis = dueDateMillis,
                            description = desc.trim(),
                            status = "unpaid",
                            isDemo = false
                        )
                    )
                    showAddDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )

        if (showDuePicker) {
            val dpState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDuePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { dueDateMillis = it }
                        showDuePicker = false
                    }) { Text(strings.save) }
                },
                dismissButton = {
                    TextButton(onClick = { showDuePicker = false }) { Text(strings.cancel) }
                }
            ) { DatePicker(state = dpState) }
        }
    }

    paymentTarget?.let { rec ->
        var amtText by remember {
            mutableStateOf(
                if (rec.remainingAmount % 1.0 == 0.0) rec.remainingAmount.toLong().toString()
                else rec.remainingAmount.toString()
            )
        }
        var note by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { paymentTarget = null },
            title = { Text("${strings.recordPayment} • ${rec.personName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amtText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amtText = it },
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
                    val amt = amtText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0 || amt > rec.remainingAmount + 0.01) {
                        err = strings.invalidAmountErr
                        return@Button
                    }
                    onAddPayment(rec, amt.coerceAtMost(rec.remainingAmount), note)
                    paymentTarget = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { paymentTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}
