package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryItem
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.ExpenseRed
import com.example.util.CategoryIconMapper

private data class ExpenseQuickPreset(
    val nameBn: String,
    val nameEn: String,
    val iconKey: String,
    val colorHex: Long
)

private val quickExpensePresets = listOf(
    ExpenseQuickPreset("কাঁচাবাজার", "Groceries", "shopping_cart", 0xFF10B981L),
    ExpenseQuickPreset("নাস্তা ও চা", "Tea & Snacks", "cafe", 0xFFF97316L),
    ExpenseQuickPreset("তেল / বাইক", "Fuel & Bike", "local_gas_station", 0xFF0EA5E9L),
    ExpenseQuickPreset("ওষুধ", "Medicine", "local_hospital", 0xFF06B6D4L),
    ExpenseQuickPreset("পোশাক", "Clothing", "checkroom", 0xFFEC4899L),
    ExpenseQuickPreset("ভ্রমণ", "Travel", "flight", 0xFF14B8A6L),
    ExpenseQuickPreset("সাবস্ক্রিপশন", "Subscriptions", "subscriptions", 0xFF6366F1L),
    ExpenseQuickPreset("মেরামত", "Repair", "build", 0xFFF59E0BL)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorDialog(
    existing: CategoryItem?,
    defaultType: String,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (CategoryItem) -> Unit
) {
    var type by remember(existing) { mutableStateOf(existing?.type ?: defaultType) }
    var nameBn by remember(existing) { mutableStateOf(existing?.nameBn ?: "") }
    var nameEn by remember(existing) { mutableStateOf(existing?.nameEn ?: "") }
    var iconKey by remember(existing) { mutableStateOf(existing?.iconKey ?: "restaurant") }
    var colorHex by remember(existing) { mutableLongStateOf(existing?.colorHex ?: 0xFFFF6B2CL) }
    var err by remember { mutableStateOf<String?>(null) }

    val suggestedIcons = remember(nameBn, nameEn, type) {
        CategoryIconMapper.suggestIcons("$nameBn $nameEn", type)
    }

    val titleText = when {
        existing != null && type == "expense" -> strings.editExpenseCategory
        existing != null -> strings.editCategory
        type == "expense" -> strings.addExpenseCategory
        else -> strings.addCategory
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = titleText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live preview header
                Surface(
                    color = Color(colorHex).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadgeIcon(
                            iconKey = iconKey,
                            colorHex = colorHex,
                            size = 42.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (if (strings.isBn) nameBn.ifBlank { nameEn } else nameEn.ifBlank { nameBn })
                                    .ifBlank { if (type == "expense") strings.expense else strings.income },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (type == "expense") strings.expense else strings.income,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "expense",
                        onClick = { type = "expense" },
                        label = { Text(strings.expense) }
                    )
                    FilterChip(
                        selected = type == "income",
                        onClick = { type = "income" },
                        label = { Text(strings.income) }
                    )
                }

                if (existing == null && type == "expense") {
                    Text(
                        text = strings.quickSuggestions,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickExpensePresets) { preset ->
                            AssistChip(
                                onClick = {
                                    nameBn = preset.nameBn
                                    nameEn = preset.nameEn
                                    iconKey = preset.iconKey
                                    colorHex = preset.colorHex
                                    err = null
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconMapper.iconFor(preset.iconKey),
                                        contentDescription = null,
                                        tint = Color(preset.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(if (strings.isBn) preset.nameBn else preset.nameEn)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { if (it.length <= 60) nameBn = it; err = null },
                    label = { Text(strings.nameBnLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_bn_input")
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { if (it.length <= 60) nameEn = it; err = null },
                    label = { Text(strings.nameEnLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_en_input")
                )

                // Suggested Logos Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(colorHex),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.suggestedLogos,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestedIcons, key = { "sug_${it.first}" }) { (key, vec) ->
                        val selected = key == iconKey
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) Color(colorHex)
                                    else Color(colorHex).copy(alpha = 0.14f)
                                )
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color(colorHex).copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { iconKey = key }
                                .testTag("suggested_logo_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                vec,
                                contentDescription = key,
                                tint = if (selected) Color.White else Color(colorHex),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Text(strings.allLogos, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconMapper.availableIcons.forEach { (key, vec) ->
                        val selected = key == iconKey
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color(colorHex)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) Color(colorHex) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                vec,
                                contentDescription = key,
                                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(strings.chooseColor, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconMapper.availableColors.forEach { c ->
                        val selected = c == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = c }
                        )
                    }
                }
                if (err != null) Text(err ?: "", color = ExpenseRed)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bn = nameBn.trim().ifBlank { nameEn.trim() }
                    val en = nameEn.trim().ifBlank { nameBn.trim() }
                    if (bn.isBlank() && en.isBlank()) {
                        err = strings.invalidInputErr
                        return@Button
                    }
                    onSave(
                        CategoryItem(
                            id = existing?.id ?: "",
                            userId = existing?.userId ?: "",
                            nameBn = bn,
                            nameEn = en,
                            type = type,
                            iconKey = iconKey,
                            colorHex = colorHex,
                            isDefault = existing?.isDefault ?: false,
                            createdAt = existing?.createdAt
                        )
                    )
                },
                modifier = Modifier.testTag("save_category_button")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
