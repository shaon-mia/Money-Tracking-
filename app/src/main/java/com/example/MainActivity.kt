package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOut
import com.example.data.model.FinanceTransaction
import com.example.data.repository.FinanceRepository
import com.example.data.repository.LocalFinanceDataSource
import com.example.data.repository.StorageModeStore
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.components.rememberPressBounce
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatMoney
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LendBorrowScreen
import com.example.ui.screens.MonthlyArchiveScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartInsightsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.HisabKoriTheme
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OrangePrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_TRANSACTIONS = "transactions"
const val ROUTE_BUDGET = "budget"
const val ROUTE_STATISTICS = "statistics"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_SAVINGS = "savings"
const val ROUTE_LEND_BORROW = "lend_borrow"
const val ROUTE_CATEGORIES = "categories"
const val ROUTE_CALENDAR = "calendar"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_MONTHLY_ARCHIVE = "monthly_archive"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val localDataSource = remember(context) { LocalFinanceDataSource(context.applicationContext) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var preferredStorageMode by remember { mutableStateOf(StorageModeStore.getMode(context)) }
    var isSyncingStorage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null && preferredStorageMode != StorageModeStore.MODE_LOCAL) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    val isUsingCloud = user != null && preferredStorageMode == StorageModeStore.MODE_CLOUD
    val cloudRepo = remember(user?.uid) {
        user?.let {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            FinanceRepository(db, auth)
        }
    }

    LaunchedEffect(user?.uid, isUsingCloud) {
        if (user != null && cloudRepo != null && isUsingCloud) {
            cloudRepo.seedInitialDataIfNeeded(user.uid)
            if (!StorageModeStore.isLocalMigratedTo(context, user.uid)) {
                localDataSource.seedInitialDataIfNeeded(LocalFinanceDataSource.LOCAL_USER_ID)
                val localNonDemoJson = localDataSource.exportNonDemoBackupJsonIfAny()
                if (localNonDemoJson != null) {
                    cloudRepo.restoreBackupJson(user.uid, localNonDemoJson)
                }
                StorageModeStore.markLocalMigrated(context, user.uid)
            }
        }
    }

    if (!isUsingCloud || user == null || cloudRepo == null) {
        key(LocalFinanceDataSource.LOCAL_USER_ID) {
            val viewModel: FinanceViewModel = viewModel(
                key = LocalFinanceDataSource.LOCAL_USER_ID,
                factory = viewModelFactory {
                    initializer {
                        FinanceViewModel(localDataSource, LocalFinanceDataSource.LOCAL_USER_ID)
                    }
                }
            )
            HisabKoriAuthenticatedShell(
                viewModel = viewModel,
                isUsingCloudStorage = false,
                isSyncingStorage = isSyncingStorage,
                onSelectCloudStorage = {
                    StorageModeStore.setMode(context, StorageModeStore.MODE_CLOUD)
                    preferredStorageMode = StorageModeStore.MODE_CLOUD
                },
                onSelectOfflineLocalStorage = {
                    StorageModeStore.setMode(context, StorageModeStore.MODE_LOCAL)
                    StorageModeStore.clearLocalMigratedFlags(context)
                    preferredStorageMode = StorageModeStore.MODE_LOCAL
                },
                onSaveCopyToPhoneStorage = {
                    viewModel.showMessage(
                        if (viewModel.uiState.value.preferences.language == "bn") {
                            "ফোনের লোকাল স্টোরেজে সংরক্ষিত আছে!"
                        } else {
                            "Already saved in phone local storage!"
                        }
                    )
                },
                onUploadLocalToGmail = {
                    if (user != null && cloudRepo != null) {
                        scope.launch {
                            isSyncingStorage = true
                            localDataSource.seedInitialDataIfNeeded(LocalFinanceDataSource.LOCAL_USER_ID)
                            val json = localDataSource.exportNonDemoBackupJsonIfAny()
                                ?: localDataSource.exportBackupJson(LocalFinanceDataSource.LOCAL_USER_ID).getOrNull()
                            if (json != null) {
                                cloudRepo.seedInitialDataIfNeeded(user.uid)
                                cloudRepo.restoreBackupJson(user.uid, json)
                                StorageModeStore.markLocalMigrated(context, user.uid)
                                StorageModeStore.setMode(context, StorageModeStore.MODE_CLOUD)
                                preferredStorageMode = StorageModeStore.MODE_CLOUD
                            }
                            isSyncingStorage = false
                        }
                    }
                },
                onSignOut = {
                    if (user != null) {
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                }
            )
        }
    } else {
        key(user.uid) {
            val viewModel: FinanceViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        FinanceViewModel(cloudRepo, user.uid)
                    }
                }
            )
            HisabKoriAuthenticatedShell(
                viewModel = viewModel,
                isUsingCloudStorage = true,
                isSyncingStorage = isSyncingStorage,
                onSelectCloudStorage = {
                    StorageModeStore.setMode(context, StorageModeStore.MODE_CLOUD)
                    preferredStorageMode = StorageModeStore.MODE_CLOUD
                },
                onSelectOfflineLocalStorage = {
                    scope.launch {
                        isSyncingStorage = true
                        val cloudJson = cloudRepo.exportBackupJson(user.uid).getOrNull()
                        if (cloudJson != null) {
                            localDataSource.seedInitialDataIfNeeded(LocalFinanceDataSource.LOCAL_USER_ID)
                            localDataSource.restoreBackupJson(LocalFinanceDataSource.LOCAL_USER_ID, cloudJson)
                        }
                        StorageModeStore.clearLocalMigratedFlags(context)
                        StorageModeStore.setMode(context, StorageModeStore.MODE_LOCAL)
                        preferredStorageMode = StorageModeStore.MODE_LOCAL
                        isSyncingStorage = false
                    }
                },
                onSaveCopyToPhoneStorage = {
                    scope.launch {
                        isSyncingStorage = true
                        val res = cloudRepo.exportBackupJson(user.uid)
                        val isBn = viewModel.uiState.value.preferences.language == "bn"
                        res.onSuccess { json ->
                            localDataSource.seedInitialDataIfNeeded(LocalFinanceDataSource.LOCAL_USER_ID)
                            localDataSource.restoreBackupJson(LocalFinanceDataSource.LOCAL_USER_ID, json)
                            viewModel.showMessage(
                                if (isBn) "সব ডাটা ফোনের লোকাল স্টোরেজে সেভ করা হয়েছে!" else "All data saved to phone local storage!"
                            )
                        }.onFailure { err ->
                            viewModel.showMessage(err.localizedMessage ?: "Failed to save locally")
                        }
                        isSyncingStorage = false
                    }
                },
                onUploadLocalToGmail = {},
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabKoriAuthenticatedShell(
    viewModel: FinanceViewModel,
    isUsingCloudStorage: Boolean,
    isSyncingStorage: Boolean,
    onSelectCloudStorage: () -> Unit,
    onSelectOfflineLocalStorage: () -> Unit,
    onSaveCopyToPhoneStorage: () -> Unit,
    onUploadLocalToGmail: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    val prefs = state.preferences
    val strings = remember(prefs.language) { AppStrings(prefs.language == "bn") }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val shellContext = LocalContext.current

    LaunchedEffect(viewModel.currentUserId) {
        if (StorageModeStore.needsCatalogUpgrade(shellContext, viewModel.currentUserId, targetVersion = 2)) {
            viewModel.syncMissingDefaultCategories()
            StorageModeStore.markCatalogUpgraded(shellContext, viewModel.currentUserId, targetVersion = 2)
        }
    }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    HisabKoriTheme(themeMode = prefs.themeMode, textScale = prefs.textScale) {
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@HisabKoriTheme
        }

        if (prefs.pinEnabled && !prefs.pinHash.isNullOrBlank() && !isUnlocked) {
            AppLockScreen(
                strings = strings,
                biometricEnabled = prefs.biometricLockEnabled,
                onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                onBiometricSuccess = { viewModel.unlockApp() }
            )
            return@HisabKoriTheme
        }

        val navController = rememberNavController()
        val navBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStack?.destination?.route ?: ROUTE_HOME

        val primaryTabs = remember(strings) {
            listOf(
                NavTab(ROUTE_HOME, strings.navHome, Icons.Rounded.Home, "nav_home"),
                NavTab(ROUTE_TRANSACTIONS, strings.navTransactions, Icons.AutoMirrored.Rounded.ReceiptLong, "nav_transactions"),
                NavTab(ROUTE_BUDGET, strings.navBudget, Icons.Rounded.PieChart, "nav_budget"),
                NavTab(ROUTE_STATISTICS, strings.navStats, Icons.Rounded.BarChart, "nav_statistics"),
                NavTab(ROUTE_SETTINGS, strings.navSettings, Icons.Rounded.Settings, "nav_settings")
            )
        }
        val isPrimaryTab = primaryTabs.any { it.route == currentRoute }

        var showTransactionSheet by remember { mutableStateOf(false) }
        var transactionInitialType by remember { mutableStateOf("expense") }
        var transactionInitialTimestamp by remember { mutableStateOf<Long?>(null) }
        var editingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
        var pendingDeleteExpenseTx by remember { mutableStateOf<FinanceTransaction?>(null) }
        var txInitialTypeFilter by remember { mutableStateOf("all") }
        var txFilterTrigger by remember { mutableIntStateOf(0) }

        fun requestDeleteTransaction(txId: String) {
            val tx = state.transactions.firstOrNull { it.id == txId }
            val requirePassword = tx != null &&
                tx.type == "expense" &&
                prefs.deletePasswordEnabled &&
                !prefs.deletePasswordHash.isNullOrBlank()
            if (requirePassword) {
                pendingDeleteExpenseTx = tx
            } else {
                viewModel.deleteTransaction(txId)
                if (editingTransaction?.id == txId) {
                    showTransactionSheet = false
                    editingTransaction = null
                }
            }
        }

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        fun navigateFromDrawer(route: String) {
            drawerScope.launch { drawerState.close() }
            if (primaryTabs.any { it.route == route }) {
                navigateTopLevel(route)
            } else {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }

        val brandTitle = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold)) {
                append("Money")
            }
            withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.ExtraBold)) {
                append("Tracker")
            }
        }

        val originalLayoutDirection = LocalLayoutDirection.current
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    drawerShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = brandTitle,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = strings.appSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        // Core navigation options (Home, Transactions, Budget, Statistics, Settings)
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    if (tab.route == ROUTE_TRANSACTIONS) {
                                        txInitialTypeFilter = "all"
                                        txFilterTrigger++
                                    }
                                    navigateFromDrawer(tab.route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (selected) OrangePrimary else MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                shape = CircleShape,
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedTextColor = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )

                        val drawerTools = listOf(
                            Triple(strings.monthlyRecords, Icons.Rounded.CalendarMonth, ROUTE_MONTHLY_ARCHIVE),
                            Triple(strings.viewExpenses, Icons.AutoMirrored.Rounded.ReceiptLong, "expenses_only"),
                            Triple(strings.categories, Icons.Rounded.Category, ROUTE_CATEGORIES),
                            Triple(strings.lendBorrow, Icons.Rounded.Handshake, ROUTE_LEND_BORROW),
                            Triple(strings.savings, Icons.Rounded.Savings, ROUTE_SAVINGS),
                            Triple(strings.calendar, Icons.Rounded.CalendarMonth, ROUTE_CALENDAR),
                            Triple(strings.smartInsights, Icons.Rounded.AutoAwesome, ROUTE_INSIGHTS)
                        )
                        drawerTools.forEach { (label, icon, route) ->
                            val selected = currentRoute == route
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    if (route == "expenses_only") {
                                        drawerScope.launch { drawerState.close() }
                                        txInitialTypeFilter = "expense"
                                        txFilterTrigger++
                                        navigateTopLevel(ROUTE_TRANSACTIONS)
                                    } else {
                                        navigateFromDrawer(route)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (selected) OrangePrimary else MaterialTheme.colorScheme.onBackground
                                    )
                                },
                                shape = CircleShape,
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedContainerColor = Color.Transparent,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedTextColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 700.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail && isPrimaryTab) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            header = {
                                FloatingActionButton(
                                    onClick = {
                                        editingTransaction = null
                                        transactionInitialType = "expense"
                                        transactionInitialTimestamp = null
                                        showTransactionSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .testTag("add_transaction_fab")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = strings.addTransaction)
                                }
                            }
                        ) {
                            primaryTabs.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets.navigationBars,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            if (isPrimaryTab) {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    title = {
                                        Text(
                                            text = brandTitle,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    },
                                    actions = {
                                        Surface(
                                            onClick = { drawerScope.launch { drawerState.open() } },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface,
                                            shadowElevation = 1.dp,
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(44.dp)
                                                .testTag("open_modules_menu")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Menu,
                                                    contentDescription = strings.moreMenuDesc,
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        floatingActionButton = {
                            AnimatedVisibility(
                                visible = !useRail && currentRoute == ROUTE_TRANSACTIONS,
                                enter = fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.86f, animationSpec = tween(300, easing = FastOutSlowInEasing)),
                                exit = fadeOut(tween(200)) +
                                    scaleOut(targetScale = 0.86f, animationSpec = tween(220))
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        editingTransaction = null
                                        transactionInitialType = "expense"
                                        transactionInitialTimestamp = null
                                        showTransactionSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("add_transaction_fab")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = strings.addTransaction)
                                }
                            }
                        },
                        bottomBar = {
                            if (!useRail && currentRoute == ROUTE_HOME) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp,
                                    shadowElevation = 8.dp,
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val (incInteraction, incBounce) = rememberPressBounce(pressedScale = 0.96f)
                                    val (expInteraction, expBounce) = rememberPressBounce(pressedScale = 0.96f)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                editingTransaction = null
                                                transactionInitialType = "income"
                                                transactionInitialTimestamp = null
                                                showTransactionSheet = true
                                            },
                                            interactionSource = incInteraction,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = IncomeGreen,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = incBounce
                                                .weight(1f)
                                                .height(58.dp)
                                                .testTag("bottom_income_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = strings.income,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                editingTransaction = null
                                                transactionInitialType = "expense"
                                                transactionInitialTimestamp = null
                                                showTransactionSheet = true
                                            },
                                            interactionSource = expInteraction,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = OrangePrimary,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = expBounce
                                                .weight(1f)
                                                .height(58.dp)
                                                .testTag("bottom_expense_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Remove,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = strings.expense,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_HOME,
                                enterTransition = {
                                    fadeIn(animationSpec = tween(340, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(animationSpec = tween(360, easing = FastOutSlowInEasing)) { it / 14 }
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(260)) +
                                        slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -it / 18 }
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(340, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(animationSpec = tween(360, easing = FastOutSlowInEasing)) { -it / 14 }
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(260)) +
                                        slideOutHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 14 }
                                },
                                modifier = Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxHeight()
                            ) {
                                composable(ROUTE_HOME) {
                                    HomeDashboardScreen(
                                        state = state,
                                        strings = strings,
                                        onToggleHideBalance = {
                                            viewModel.updatePreferences { it.copy(hideBalances = !it.hideBalances) }
                                        },
                                        onClearDemoData = { viewModel.clearDemoData() },
                                        onViewExpenses = {
                                            txInitialTypeFilter = "expense"
                                            txFilterTrigger++
                                            navigateTopLevel(ROUTE_TRANSACTIONS)
                                        },
                                        onOpenLendBorrow = { navController.navigate(ROUTE_LEND_BORROW) },
                                        onOpenMonthlyArchive = { navController.navigate(ROUTE_MONTHLY_ARCHIVE) }
                                    )
                                }

                                composable(ROUTE_TRANSACTIONS) {
                                    TransactionsScreen(
                                        state = state,
                                        strings = strings,
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            transactionInitialType = tx.type
                                            transactionInitialTimestamp = null
                                            showTransactionSheet = true
                                        },
                                        onDeleteTransaction = { requestDeleteTransaction(it) },
                                        onOpenCalendar = { navController.navigate(ROUTE_CALENDAR) },
                                        onOpenMonthlyArchive = { navController.navigate(ROUTE_MONTHLY_ARCHIVE) },
                                        initialTypeFilter = txInitialTypeFilter,
                                        filterTrigger = txFilterTrigger
                                    )
                                }

                                composable(ROUTE_BUDGET) {
                                    BudgetScreen(
                                        state = state,
                                        strings = strings,
                                        onSaveBudget = { viewModel.saveBudget(it) },
                                        onDeleteBudget = { viewModel.deleteBudget(it) }
                                    )
                                }

                                composable(ROUTE_STATISTICS) {
                                    StatisticsScreen(
                                        state = state,
                                        strings = strings,
                                        onOpenInsights = { navController.navigate(ROUTE_INSIGHTS) }
                                    )
                                }

                                composable(ROUTE_SETTINGS) {
                                    SettingsScreen(
                                        state = state,
                                        strings = strings,
                                        isUsingCloudStorage = isUsingCloudStorage,
                                        isSyncingStorage = isSyncingStorage,
                                        onSelectCloudStorage = onSelectCloudStorage,
                                        onSelectOfflineLocalStorage = onSelectOfflineLocalStorage,
                                        onSaveCopyToPhoneStorage = onSaveCopyToPhoneStorage,
                                        onUploadLocalToGmail = onUploadLocalToGmail,
                                        onUpdatePreferences = { viewModel.updatePreferences(it) },
                                        onSetPin = { viewModel.setPin(it) },
                                        onDisablePin = { viewModel.disablePin() },
                                        onSetDeletePassword = { viewModel.setDeletePassword(it) },
                                        onVerifyDeletePassword = { viewModel.verifyDeletePassword(it) },
                                        onDisableDeletePassword = { viewModel.disableDeletePassword() },
                                        onClearDemoData = { viewModel.clearDemoData() },
                                        onExportBackupJson = { viewModel.exportBackupJson() },
                                        onRestoreBackupJson = { viewModel.restoreBackupJson(it) },
                                        onOpenSavings = { navController.navigate(ROUTE_SAVINGS) },
                                        onOpenLendBorrow = { navController.navigate(ROUTE_LEND_BORROW) },
                                        onOpenCategories = { navController.navigate(ROUTE_CATEGORIES) },
                                        onOpenCalendar = { navController.navigate(ROUTE_CALENDAR) },
                                        onOpenInsights = { navController.navigate(ROUTE_INSIGHTS) },
                                        onSignOut = onSignOut
                                    )
                                }

                                composable(ROUTE_SAVINGS) {
                                    SavingsScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navController.popBackStack() },
                                        onSaveMonthlyGoal = { goal ->
                                            viewModel.updatePreferences { it.copy(monthlySavingsGoal = goal) }
                                        },
                                        onSaveGoal = { viewModel.saveSavingsGoal(it) },
                                        onAddContribution = { goal, amt, note ->
                                            viewModel.addSavingsContribution(goal, amt, note)
                                        },
                                        onDeleteGoal = { viewModel.deleteSavingsGoal(it) }
                                    )
                                }

                                composable(ROUTE_LEND_BORROW) {
                                    LendBorrowScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navController.popBackStack() },
                                        onSaveRecord = { viewModel.saveLendBorrow(it) },
                                        onAddPayment = { rec, amt, note ->
                                            viewModel.addLendBorrowPayment(rec, amt, note)
                                        },
                                        onDeleteRecord = { viewModel.deleteLendBorrow(it) }
                                    )
                                }

                                composable(ROUTE_CATEGORIES) {
                                    CategoriesScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navController.popBackStack() },
                                        onSaveCategory = { viewModel.saveCategory(it) },
                                        onDeleteCategory = { viewModel.deleteCategory(it) }
                                    )
                                }

                                composable(ROUTE_CALENDAR) {
                                    CalendarScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navController.popBackStack() },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            transactionInitialType = tx.type
                                            showTransactionSheet = true
                                        }
                                    )
                                }

                                composable(ROUTE_INSIGHTS) {
                                    SmartInsightsScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(ROUTE_MONTHLY_ARCHIVE) {
                                    MonthlyArchiveScreen(
                                        state = state,
                                        strings = strings,
                                        onBack = { navController.popBackStack() },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            transactionInitialType = tx.type
                                            transactionInitialTimestamp = null
                                            showTransactionSheet = true
                                        },
                                        onDeleteTransaction = { requestDeleteTransaction(it) },
                                        onAddExpenseForMonth = { monthTs ->
                                            editingTransaction = null
                                            transactionInitialType = "expense"
                                            transactionInitialTimestamp = monthTs
                                            showTransactionSheet = true
                                        }
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

        if (showTransactionSheet) {
            TransactionEditorSheet(
                existing = editingTransaction,
                initialType = transactionInitialType,
                initialTimestampMillis = transactionInitialTimestamp,
                categories = state.categories,
                currencyCode = prefs.currencyCode,
                language = prefs.language,
                strings = strings,
                isSaving = isSaving,
                onDismiss = { showTransactionSheet = false },
                onSave = { tx, docId ->
                    viewModel.saveTransaction(tx, docId) { ok ->
                        if (ok) showTransactionSheet = false
                    }
                },
                onDelete = { txId -> requestDeleteTransaction(txId) },
                onSaveCategory = { cat, onDone ->
                    viewModel.saveCategoryWithResult(cat, onDone)
                }
            )
        }

        val targetTx = pendingDeleteExpenseTx
        if (targetTx != null) {
            var pwInput by remember(targetTx.id) { mutableStateOf("") }
            var showPw by remember(targetTx.id) { mutableStateOf(false) }
            var pwError by remember(targetTx.id) { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { pendingDeleteExpenseTx = null },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = ExpenseRed
                    )
                },
                title = {
                    Text(
                        text = strings.verifyDeletePasswordTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${targetTx.categoryDisplayName(prefs.language)} • ${formatMoney(targetTx.amount, prefs.currencyCode, prefs.hideBalances)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                        OutlinedTextField(
                            value = pwInput,
                            onValueChange = {
                                pwInput = it
                                pwError = null
                            },
                            label = { Text(strings.enterPasswordLabel) },
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("verify_delete_expense_password_input")
                        )
                        if (pwError != null) {
                            Text(
                                text = pwError ?: "",
                                color = ExpenseRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (viewModel.verifyDeletePassword(pwInput)) {
                                viewModel.deleteTransaction(targetTx.id)
                                if (editingTransaction?.id == targetTx.id) {
                                    showTransactionSheet = false
                                    editingTransaction = null
                                }
                                pendingDeleteExpenseTx = null
                                viewModel.showMessage(strings.expenseDeletedMsg)
                            } else {
                                pwError = strings.wrongPasswordErr
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        modifier = Modifier.testTag("confirm_delete_expense_password_button")
                    ) {
                        Text(strings.delete, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteExpenseTx = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
    }
}
