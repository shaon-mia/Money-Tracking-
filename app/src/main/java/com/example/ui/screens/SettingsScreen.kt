package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.alarm.AlarmScheduler
import com.example.alarm.TextAlarmItem
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.UserPreferences
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceDashboardState
import com.example.util.ReportExporter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: FinanceDashboardState,
    strings: AppStrings,
    isUsingCloudStorage: Boolean,
    isSyncingStorage: Boolean,
    onSelectCloudStorage: () -> Unit,
    onSelectOfflineLocalStorage: () -> Unit,
    onSaveCopyToPhoneStorage: () -> Unit,
    onUploadLocalToGmail: () -> Unit,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onSetDeletePassword: (String) -> Unit,
    onVerifyDeletePassword: (String) -> Boolean,
    onDisableDeletePassword: () -> Unit,
    onClearDemoData: () -> Unit,
    onExportBackupJson: suspend () -> Result<String>,
    onRestoreBackupJson: (String) -> Unit,
    onOpenSavings: () -> Unit,
    onOpenLendBorrow: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInsights: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val prefs = state.preferences
    val user = Firebase.auth.currentUser

    var showPinDialog by remember { mutableStateOf(false) }
    var showSetDeletePasswordDialog by remember { mutableStateOf(false) }
    var showDisableDeletePasswordDialog by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var alarmConfig by remember { mutableStateOf(AlarmScheduler.loadState(context)) }

    var newTextAlarmBody by remember { mutableStateOf("") }
    var newTextAlarmHour by remember { mutableIntStateOf(9) }
    var newTextAlarmMinute by remember { mutableIntStateOf(0) }
    var newTextAlarmRepeatDaily by remember { mutableStateOf(true) }
    var showTextAlarmTimePicker by remember { mutableStateOf(false) }

    var pendingTestNotification by remember { mutableStateOf<Pair<String, String>?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingTestNotification?.let { (t, m) ->
                AlarmScheduler.showAlarmNotification(context, 9001, t, m)
                Toast.makeText(context, strings.alarmTestSentMsg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, strings.notifPermissionMsg, Toast.LENGTH_SHORT).show()
        }
        pendingTestNotification = null
    }

    fun requestNotifPermissionIfNeeded(testTitle: String? = null, testMsg: String? = null): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                if (testTitle != null && testMsg != null) {
                    pendingTestNotification = testTitle to testMsg
                }
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return false
            }
        }
        return true
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { jsonText ->
                onRestoreBackupJson(jsonText)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.navSettings,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Data Storage Mode (Gmail Account vs Offline Phone Local Storage)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.storageModeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isUsingCloudStorage,
                            onClick = {
                                if (user == null) {
                                    authError = null
                                    isSigningIn = true
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isSigningIn = false
                                            onSelectCloudStorage()
                                        },
                                        onAuthError = { err ->
                                            isSigningIn = false
                                            authError = err
                                        },
                                        scope = scope,
                                        onAuthCancelled = { isSigningIn = false }
                                    )
                                } else {
                                    onSelectCloudStorage()
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(strings.storageGmailMode, maxLines = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("storage_mode_gmail_chip")
                        )

                        FilterChip(
                            selected = !isUsingCloudStorage,
                            onClick = onSelectOfflineLocalStorage,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(strings.storageOfflineMode, maxLines = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("storage_mode_offline_chip")
                        )
                    }

                    HorizontalDivider()

                    if (user != null && isUsingCloudStorage) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName ?: strings.appTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = IncomeGreen.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = strings.cloudSyncActive,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = IncomeGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                if (!user.email.isNullOrBlank()) {
                                    Text(
                                        text = user.email ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = strings.signOut, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.signOut)
                            }
                        }

                        OutlinedButton(
                            onClick = onSaveCopyToPhoneStorage,
                            enabled = !isSyncingStorage,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("save_copy_phone_storage_button")
                        ) {
                            if (isSyncingStorage) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.saveToPhoneStorageBtn, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (user != null) Icons.Rounded.PhoneAndroid else Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.localAccountTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (!user?.email.isNullOrBlank()) {
                                        "${strings.localAccountDesc} (${user?.email})"
                                    } else {
                                        strings.localAccountDesc
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (authError != null) {
                            Text(
                                text = authError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed
                            )
                        }

                        if (user == null) {
                            Button(
                                onClick = {
                                    authError = null
                                    isSigningIn = true
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isSigningIn = false
                                            onSelectCloudStorage()
                                        },
                                        onAuthError = { err ->
                                            isSigningIn = false
                                            authError = err
                                        },
                                        scope = scope,
                                        onAuthCancelled = { isSigningIn = false }
                                    )
                                },
                                enabled = !isSigningIn,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_google_sign_in_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(strings.signInWithGoogle, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onUploadLocalToGmail,
                                    enabled = !isSyncingStorage,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("upload_local_to_gmail_button")
                                ) {
                                    if (isSyncingStorage) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.uploadLocalToGmailBtn, maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onSignOut,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = strings.signOut, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language, Theme, Text Size, Currency
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.language == "bn",
                            onClick = { onUpdatePreferences { it.copy(language = "bn") } },
                            label = { Text("বাংলা (Default)") },
                            modifier = Modifier.testTag("lang_bn_chip")
                        )
                        FilterChip(
                            selected = prefs.language == "en",
                            onClick = { onUpdatePreferences { it.copy(language = "en") } },
                            label = { Text("English") },
                            modifier = Modifier.testTag("lang_en_chip")
                        )
                    }

                    HorizontalDivider()

                    Text(strings.themeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("light" to strings.light, "dark" to strings.dark, "system" to strings.system).forEach { (key, label) ->
                            FilterChip(
                                selected = prefs.themeMode == key,
                                onClick = { onUpdatePreferences { it.copy(themeMode = key) } },
                                label = { Text(label) }
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(strings.textSizeLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("small" to strings.small, "medium" to strings.medium, "large" to strings.large).forEach { (key, label) ->
                            FilterChip(
                                selected = prefs.textScale == key,
                                onClick = { onUpdatePreferences { it.copy(textScale = key) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Quick Module Menu
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    SettingsActionRow(strings.savings, Icons.Rounded.Savings, onOpenSavings)
                    SettingsActionRow(strings.lendBorrow, Icons.Rounded.Handshake, onOpenLendBorrow)
                    SettingsActionRow(strings.categories, Icons.Rounded.Category, onOpenCategories)
                    SettingsActionRow(strings.calendar, Icons.Rounded.CalendarMonth, onOpenCalendar)
                    SettingsActionRow(strings.smartInsights, Icons.Rounded.AutoAwesome, onOpenInsights)
                }
            }
        }

        // Text Alarm Section (Notification Sound Only)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AlarmAdd,
                            contentDescription = strings.textAlarmTitle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.textAlarmTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.textAlarmSoundOnlyNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newTextAlarmBody,
                        onValueChange = { if (it.length <= 180) newTextAlarmBody = it },
                        label = { Text(strings.textAlarmHint) },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_text_alarm_input")
                    )

                    val presets = if (strings.isBn) {
                        listOf("আজকের খরচ লিখুন", "বিদ্যুৎ বিল দিন", "বাসা ভাড়া দিন", "ধার পরিশোধ")
                    } else {
                        listOf("Log today's expenses", "Pay utility bill", "Pay house rent", "Settle loan")
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presets.forEach { preset ->
                            AssistChip(
                                onClick = { newTextAlarmBody = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showTextAlarmTimePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_alarm_time_button")
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(AlarmScheduler.formatAlarmTime(newTextAlarmHour, newTextAlarmMinute, strings.isBn))
                        }
                        FilterChip(
                            selected = newTextAlarmRepeatDaily,
                            onClick = { newTextAlarmRepeatDaily = !newTextAlarmRepeatDaily },
                            label = {
                                Text(if (newTextAlarmRepeatDaily) strings.repeatDailyLabel else strings.oneTimeLabel)
                            }
                        )
                    }

                    Button(
                        onClick = {
                            val cleanText = newTextAlarmBody.trim()
                            if (cleanText.isEmpty()) {
                                Toast.makeText(context, strings.invalidInputErr, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            requestNotifPermissionIfNeeded()
                            val newItem = TextAlarmItem(
                                text = cleanText,
                                hour = newTextAlarmHour,
                                minute = newTextAlarmMinute,
                                repeatDaily = newTextAlarmRepeatDaily,
                                enabled = true
                            )
                            val updated = AlarmScheduler.saveAndScheduleAll(
                                context,
                                alarmConfig.copy(textAlarms = listOf(newItem) + alarmConfig.textAlarms)
                            )
                            alarmConfig = updated
                            newTextAlarmBody = ""
                            Toast.makeText(context, strings.alarmSavedMsg, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_text_alarm_button")
                    ) {
                        Icon(Icons.Rounded.AlarmAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.addTextAlarmBtn, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = strings.savedTextAlarms,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (alarmConfig.textAlarms.isEmpty()) {
                        Text(
                            text = strings.noTextAlarms,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            alarmConfig.textAlarms.forEach { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.text,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${AlarmScheduler.formatAlarmTime(item.hour, item.minute, strings.isBn)} • ${if (item.repeatDaily) strings.repeatDailyLabel else strings.oneTimeLabel}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            if (requestNotifPermissionIfNeeded(strings.savedTextAlarms, item.text)) {
                                                AlarmScheduler.showAlarmNotification(
                                                    context = context,
                                                    notificationId = 9100 + (item.id.hashCode() and 0x0FFF),
                                                    title = "MoneyTracker • ${strings.alarmSectionTitle}",
                                                    message = item.text
                                                )
                                                Toast.makeText(context, strings.alarmTestSentMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsActive,
                                                contentDescription = strings.testAlarmBtn,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Switch(
                                            checked = item.enabled,
                                            onCheckedChange = { checked ->
                                                if (checked) requestNotifPermissionIfNeeded()
                                                val updatedList = alarmConfig.textAlarms.map {
                                                    if (it.id == item.id) it.copy(enabled = checked) else it
                                                }
                                                alarmConfig = AlarmScheduler.saveAndScheduleAll(
                                                    context,
                                                    alarmConfig.copy(textAlarms = updatedList)
                                                )
                                            }
                                        )
                                        IconButton(onClick = {
                                            val updatedList = alarmConfig.textAlarms.filterNot { it.id == item.id }
                                            alarmConfig = AlarmScheduler.saveAndScheduleAll(
                                                context,
                                                alarmConfig.copy(textAlarms = updatedList)
                                            )
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = strings.delete,
                                                tint = ExpenseRed
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

        // Privacy & Security
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.privacySecurity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.hideBalancesLabel, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = prefs.hideBalances,
                            onCheckedChange = { checked ->
                                onUpdatePreferences { it.copy(hideBalances = checked) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.pinLockLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = prefs.pinEnabled,
                            onCheckedChange = { checked ->
                                if (checked) showPinDialog = true else onDisablePin()
                            }
                        )
                    }

                    if (prefs.pinEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.biometricLockLabel, style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = prefs.biometricLockEnabled,
                                onCheckedChange = { checked ->
                                    onUpdatePreferences { it.copy(biometricLockEnabled = checked) }
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Password, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(strings.deleteExpensePasswordLabel, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = strings.deleteExpensePasswordDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = prefs.deletePasswordEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showSetDeletePasswordDialog = true
                                } else {
                                    if (!prefs.deletePasswordHash.isNullOrBlank()) {
                                        showDisableDeletePasswordDialog = true
                                    } else {
                                        onDisableDeletePassword()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("delete_expense_password_switch")
                        )
                    }

                    if (prefs.deletePasswordEnabled) {
                        OutlinedButton(
                            onClick = { showSetDeletePasswordDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("change_delete_password_button")
                        ) {
                            Icon(Icons.Rounded.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.changeDeletePasswordBtn)
                        }
                    }
                }
            }
        }

        // Data Backup, Restore & Export
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.backupRestore, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            scope.launch {
                                val res = onExportBackupJson()
                                res.onSuccess { json ->
                                    val file = ReportExporter.writeTextFileToCache(
                                        context,
                                        "HisabKori_Backup_${System.currentTimeMillis()}.json",
                                        json
                                    )
                                    ReportExporter.shareFile(context, file, "application/json", strings.backupData)
                                }.onFailure {
                                    Toast.makeText(context, it.localizedMessage ?: "Backup failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.backupData)
                    }

                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.restoreBackup)
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val csv = ReportExporter.buildCsvContent(state.transactions, prefs.currencyCode, prefs.language)
                                val file = ReportExporter.writeTextFileToCache(
                                    context,
                                    "HisabKori_PersonalData_${System.currentTimeMillis()}.csv",
                                    csv
                                )
                                ReportExporter.shareFile(context, file, "text/csv", strings.exportPersonalData)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.exportPersonalData)
                    }

                    if (state.hasDemoData) {
                        OutlinedButton(
                            onClick = onClearDemoData,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.clearDemo)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showTextAlarmTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = newTextAlarmHour,
            initialMinute = newTextAlarmMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTextAlarmTimePicker = false },
            title = { Text(strings.alarmTimeLabel, fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(onClick = {
                    newTextAlarmHour = timeState.hour
                    newTextAlarmMinute = timeState.minute
                    showTextAlarmTimePicker = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showTextAlarmTimePicker = false }) { Text(strings.cancel) }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.setPinTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (err != null) Text(err ?: "", color = ExpenseRed)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length != 4) {
                        err = strings.invalidInputErr
                        return@Button
                    }
                    onSetPin(pinInput)
                    showPinDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showSetDeletePasswordDialog) {
        val isChangingExisting = prefs.deletePasswordEnabled && !prefs.deletePasswordHash.isNullOrBlank()
        var currentPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var showPw by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetDeletePasswordDialog = false },
            title = {
                Text(
                    text = if (isChangingExisting) strings.changeDeletePasswordBtn else strings.setDeletePasswordTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isChangingExisting) {
                        OutlinedTextField(
                            value = currentPw,
                            onValueChange = { currentPw = it; err = null },
                            label = { Text(strings.currentPasswordLabel) },
                            visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showPw = !showPw }) {
                                    Icon(
                                        imageVector = if (showPw) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it; err = null },
                        label = { Text(strings.newPasswordLabel) },
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        trailingIcon = if (!isChangingExisting) {
                            {
                                IconButton(onClick = { showPw = !showPw }) {
                                    Icon(
                                        imageVector = if (showPw) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = null
                                    )
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_delete_password_input")
                    )
                    OutlinedTextField(
                        value = confirmPw,
                        onValueChange = { confirmPw = it; err = null },
                        label = { Text(strings.confirmPasswordLabel) },
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_delete_password_input")
                    )
                    if (err != null) {
                        Text(err ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isChangingExisting && !onVerifyDeletePassword(currentPw)) {
                            err = strings.wrongPasswordErr
                            return@Button
                        }
                        if (newPw.length < 4) {
                            err = strings.passwordTooShortErr
                            return@Button
                        }
                        if (newPw != confirmPw) {
                            err = strings.passwordMismatchErr
                            return@Button
                        }
                        onSetDeletePassword(newPw)
                        showSetDeletePasswordDialog = false
                        Toast.makeText(context, strings.deletePasswordSavedMsg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("save_delete_password_button")
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showSetDeletePasswordDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showDisableDeletePasswordDialog) {
        var currentPw by remember { mutableStateOf("") }
        var showPw by remember { mutableStateOf(false) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDisableDeletePasswordDialog = false },
            title = { Text(strings.verifyDisableDeletePasswordTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPw,
                        onValueChange = { currentPw = it; err = null },
                        label = { Text(strings.currentPasswordLabel) },
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showPw = !showPw }) {
                                Icon(
                                    imageVector = if (showPw) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err != null) {
                        Text(err ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!onVerifyDeletePassword(currentPw)) {
                            err = strings.wrongPasswordErr
                            return@Button
                        }
                        onDisableDeletePassword()
                        showDisableDeletePasswordDialog = false
                        Toast.makeText(context, strings.deletePasswordDisabledMsg, Toast.LENGTH_SHORT).show()
                    }
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDeletePasswordDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    }
}
