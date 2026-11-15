package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CategoryItem
import com.example.data.model.FinanceTransaction
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.CurrencyCatalog
import com.example.ui.i18n.formatDateTime
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.ReportExporter
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
    existing: FinanceTransaction?,
    initialType: String = "expense",
    initialTimestampMillis: Long? = null,
    categories: List<CategoryItem>,
    currencyCode: String,
    language: String,
    strings: AppStrings,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (FinanceTransaction, String) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onSaveCategory: ((CategoryItem, (String?) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sessionDocId = remember(existing?.id) {
        existing?.id?.takeIf { it.isNotBlank() } ?: "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

    var type by remember(existing, initialType) { mutableStateOf(existing?.type ?: initialType) }
    var amountText by remember(existing) {
        mutableStateOf(
            if (existing != null && existing.amount > 0.0) {
                if (existing.amount % 1.0 == 0.0) existing.amount.toLong().toString() else existing.amount.toString()
            } else ""
        )
    }
    val filteredCats = remember(categories, type) { categories.filter { it.type == type } }
    var selectedCategoryId by remember(existing, type) {
        mutableStateOf(
            existing?.categoryId?.takeIf { id -> filteredCats.any { it.id == id } }
                ?: filteredCats.firstOrNull()?.id
                ?: ""
        )
    }
    var timestampMillis by remember(existing, initialTimestampMillis) {
        mutableLongStateOf(existing?.timestampMillis ?: initialTimestampMillis ?: System.currentTimeMillis())
    }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var paymentMethod by remember(existing) { mutableStateOf(existing?.paymentMethod ?: "cash") }
    var photoUri by remember(existing) { mutableStateOf(existing?.photoUri) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryEditorDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryItem?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = ReportExporter.copyUriToInternalReceipts(context, uri) ?: uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) strings.addTransaction else strings.editTransaction,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    if (existing != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(existing.id)
                            },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = strings.delete,
                                tint = ExpenseRed
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = strings.cancel)
                    }
                }
            }

            // Type segmented toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("expense" to strings.expense, "income" to strings.income).forEach { (key, label) ->
                    val selected = type == key
                    val targetBg = if (selected) {
                        if (key == "income") IncomeGreen else ExpenseRed
                    } else Color.Transparent
                    val bgColor by animateColorAsState(
                        targetValue = targetBg,
                        animationSpec = tween(340),
                        label = "tx_type_bg_$key"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(340),
                        label = "tx_type_txt_$key"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable {
                                type = key
                                val firstCat = categories.firstOrNull { it.type == key }
                                if (firstCat != null) selectedCategoryId = firstCat.id
                            }
                            .testTag("tx_type_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            // Amount input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) {
                        amountText = input
                        validationError = null
                    }
                },
                label = { Text("${strings.amount} (${CurrencyCatalog.symbolFor(currencyCode)})") },
                prefix = {
                    Text(
                        text = "${CurrencyCatalog.symbolFor(currencyCode)} ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            // Category picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (onSaveCategory != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val currentSelectedCat = filteredCats.firstOrNull { it.id == selectedCategoryId }
                        if (currentSelectedCat != null) {
                            OutlinedButton(
                                onClick = {
                                    categoryToEdit = currentSelectedCat
                                    showCategoryEditorDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("tx_edit_category_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = strings.editCategory,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = strings.editShort,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                categoryToEdit = null
                                showCategoryEditorDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("tx_add_category_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.addCategory,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = strings.newShort,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredCats.forEach { cat ->
                    val selected = cat.id == selectedCategoryId
                    val catColor = Color(cat.colorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) catColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) catColor else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedCategoryId = cat.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = com.example.util.CategoryIconMapper.iconFor(cat.iconKey),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = cat.displayName(language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Date & Time
            Text(
                text = strings.dateAndTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(formatDateTime(timestampMillis, language).substringBefore(","))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(formatDateTime(timestampMillis, language).substringAfter(", ").ifBlank { "Time" })
                }
            }

            // Payment Method
            Text(
                text = strings.paymentMethod,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "cash" to strings.cash,
                    "bank" to strings.bank,
                    "mobile_banking" to strings.mobileBanking,
                    "other" to strings.otherMethod
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = paymentMethod == key,
                        onClick = { paymentMethod = key },
                        label = { Text(label, maxLines = 1) },
                        modifier = Modifier.testTag("payment_method_$key")
                    )
                }
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 300) note = it },
                label = { Text(strings.note) },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            // Photo receipt attachment via zero-permission Photo Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoUri == null) strings.attachPhoto else strings.changePhoto)
                }
                if (photoUri != null) {
                    TextButton(onClick = { photoUri = null }) {
                        Text(strings.removePhoto, color = ExpenseRed)
                    }
                }
            }

            AnimatedVisibility(
                visible = photoUri != null,
                enter = fadeIn(tween(320)) + expandVertically(tween(320)),
                exit = fadeOut(tween(220)) + shrinkVertically(tween(220))
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = strings.attachPhoto,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            AnimatedVisibility(
                visible = validationError != null,
                enter = fadeIn(tween(280)) + expandVertically(tween(280)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Text(
                    text = validationError ?: "",
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val (saveInteraction, saveBounceModifier) = rememberPressBounce()
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0.0) {
                        validationError = strings.invalidAmountErr
                        return@Button
                    }
                    val chosenCat = filteredCats.firstOrNull { it.id == selectedCategoryId }
                        ?: filteredCats.firstOrNull()
                    if (chosenCat == null) {
                        validationError = strings.invalidInputErr
                        return@Button
                    }
                    val tx = FinanceTransaction(
                        id = existing?.id ?: "",
                        userId = existing?.userId ?: "",
                        type = type,
                        amount = parsedAmount,
                        categoryId = chosenCat.id,
                        categoryNameBn = chosenCat.nameBn,
                        categoryNameEn = chosenCat.nameEn,
                        timestampMillis = timestampMillis,
                        note = note.trim(),
                        paymentMethod = paymentMethod,
                        photoUri = photoUri,
                        clientRequestId = sessionDocId,
                        isDemo = false,
                        createdAt = existing?.createdAt
                    )
                    onSave(tx, sessionDocId)
                },
                enabled = !isSaving,
                interactionSource = saveInteraction,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = saveBounceModifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (existing == null) strings.save else strings.update,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestampMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { pickedUtc ->
                        val oldCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
                        val pickedCal = Calendar.getInstance().apply { timeInMillis = pickedUtc }
                        oldCal.set(Calendar.YEAR, pickedCal.get(Calendar.YEAR))
                        oldCal.set(Calendar.MONTH, pickedCal.get(Calendar.MONTH))
                        oldCal.set(Calendar.DAY_OF_MONTH, pickedCal.get(Calendar.DAY_OF_MONTH))
                        timestampMillis = oldCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(Calendar.MINUTE, timeState.minute)
                    timestampMillis = cal.timeInMillis
                    showTimePicker = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(strings.cancel) }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showCategoryEditorDialog && onSaveCategory != null) {
        CategoryEditorDialog(
            existing = categoryToEdit,
            defaultType = type,
            strings = strings,
            onDismiss = { showCategoryEditorDialog = false },
            onSave = { savedCat ->
                onSaveCategory(savedCat) { newId ->
                    if (!newId.isNullOrBlank()) {
                        selectedCategoryId = newId
                    }
                    showCategoryEditorDialog = false
                }
            }
        )
    }
}
