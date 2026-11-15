package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.CategoryBadgeIcon
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.OrangePrimary
import com.example.ui.viewmodel.FinanceDashboardState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    onBack: () -> Unit,
    onSaveCategory: (CategoryItem) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val lang = state.preferences.language
    var selectedType by remember { mutableStateOf("expense") }
    var editingCat by remember { mutableStateOf<CategoryItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val filtered = remember(state.categories, selectedType) {
        state.categories.filter { it.type == selectedType }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text(strings.categories, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingCat = null
                    showEditor = true
                },
                containerColor = OrangePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(strings.addCategory, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_category_button")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // CoinPie Full-width Expenses | Income Underline Tab Bar
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("expense" to strings.expense, "income" to strings.income).forEach { (key, title) ->
                    val selected = selectedType == key
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = key },
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            AnimatedContent(
                targetState = selectedType,
                transitionSpec = {
                    val forward = targetState == "income"
                    (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { if (forward) it / 8 else -it / 8 })
                        .togetherWith(fadeOut(tween(220)) + slideOutHorizontally(tween(240)) { if (forward) -it / 8 else it / 8 })
                },
                label = "categories_tab_anim"
            ) { currentTabType ->
                val tabItems = remember(state.categories, currentTabType) {
                    state.categories.filter { it.type == currentTabType }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tabItems, key = { it.id }) { cat ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    editingCat = cat
                                    showEditor = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryBadgeIcon(iconKey = cat.iconKey, colorHex = cat.colorHex, size = 44.dp)
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.displayName(lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val alt = if (lang == "bn") cat.nameEn else cat.nameBn
                                    if (alt.isNotBlank() && alt != cat.displayName(lang)) {
                                        Text(
                                            text = alt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingCat = cat
                                        showEditor = true
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("edit_category_${cat.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = strings.editCategory,
                                        tint = OrangePrimary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCategory(cat.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            existing = editingCat,
            defaultType = selectedType,
            strings = strings,
            onDismiss = { showEditor = false },
            onSave = { cat ->
                onSaveCategory(cat)
                showEditor = false
            }
        )
    }
}
