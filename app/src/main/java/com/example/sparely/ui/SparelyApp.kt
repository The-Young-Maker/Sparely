package com.example.sparely.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import com.example.sparely.ui.utils.DateUtils
import com.example.sparely.ui.utils.filterCurrencyInput
import com.example.sparely.ui.utils.toSafeDouble
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Instant
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sparely.domain.model.VaultArchivePrompt
import com.example.sparely.ui.screens.BudgetScreen
import com.example.sparely.ui.screens.ChallengesScreen
import com.example.sparely.ui.screens.DashboardScreen
import com.example.sparely.ui.screens.ExpenseEntryScreen
import com.example.sparely.ui.screens.FinancialHealthScreen
import com.example.sparely.ui.screens.HistoryScreen
import com.example.sparely.ui.screens.MainAccountScreen
import com.example.sparely.ui.screens.OnboardingScreen
import com.example.sparely.ui.screens.RecurringExpensesScreen
import com.example.sparely.ui.screens.SettingsScreen
import com.example.sparely.ui.screens.VaultHistoryScreen
import com.example.sparely.ui.screens.VaultManagementScreen
import com.example.sparely.ui.screens.VaultTransfersScreen
import com.example.sparely.ui.screens.CreditCardsScreen
import com.example.sparely.ui.screens.InsightsScreen
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.viewmodel.VaultViewModel
import com.example.sparely.ui.viewmodel.VaultViewModelFactory
import com.example.sparely.ui.viewmodel.SettingsViewModel
import com.example.sparely.ui.viewmodel.SettingsViewModelFactory
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.sparely.app.R
import com.example.sparely.ui.theme.MaterialSymbols

@Composable
fun SparelyApp(
    viewModel: SparelyViewModel,
    deepLinkDestination: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    onAuthenticateUser: ((Boolean) -> Unit) -> Unit = {}
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val context = LocalContext.current
    val app = context.applicationContext as com.example.sparely.SparelyApplication
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModelFactory(app.container)
    )
    val vaultViewModel: VaultViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = VaultViewModelFactory(app.container)
    )
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val vaultUiState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val allVaults by vaultViewModel.smartVaults.collectAsStateWithLifecycle()

    LaunchedEffect(deepLinkDestination) {
        deepLinkDestination?.let { destination ->
            navController.navigate(destination)
            onDeepLinkHandled()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (!message.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.lastDeletedExpense) {
        val deleted = uiState.lastDeletedExpense
        if (deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.history_undo_delete_message),
                actionLabel = context.getString(R.string.history_undo_delete_action),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteExpense()
            } else {
                viewModel.clearDeletedExpense()
            }
        }
    }

    if (!uiState.onboardingCompleted) {
        OnboardingScreen(
            onComplete = { profile -> viewModel.completeOnboarding(profile) },
            onImportData = { uri ->
                settingsViewModel.importData(uri, context) {
                    // Success callback
                }
            },
            onSkip = viewModel::skipOnboarding,
            snackbarHostState = snackbarHostState
        )
    } else {
        SparelyScaffold(
            navController = navController,
            snackbarHostState = snackbarHostState,
            uiState = uiState,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            settingsUiState = settingsUiState,
            vaultViewModel = vaultViewModel,
            vaultUiState = vaultUiState,
            allVaults = allVaults,
            onAuthenticateUser = onAuthenticateUser
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SparelyScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    uiState: com.example.sparely.ui.state.SparelyUiState,
    viewModel: SparelyViewModel,
    settingsViewModel: SettingsViewModel,
    settingsUiState: com.example.sparely.ui.viewmodel.SettingsUiState,
    vaultViewModel: VaultViewModel,
    vaultUiState: com.example.sparely.ui.viewmodel.VaultUiState,
    allVaults: List<com.example.sparely.domain.model.SmartVault>,
    onAuthenticateUser: ((Boolean) -> Unit) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    


    // Only show the quick actions FAB on the Dashboard screen. Other screens provide their
    // own FABs (Vaults, Recurring, etc.) and we don't want to collide with them.
    val showFab = currentDestination?.route == SparelyDestination.Dashboard.route
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (currentDestination?.route != SparelyDestination.Dashboard.route) {
                SparelyTopBar(currentDestination, navController)
            }
        },
        bottomBar = {
            SparelyBottomBar(
                currentDestination = currentDestination,
                navController = navController
            )
        },
        floatingActionButton = {
            if (showFab) {
                // Material-like stacked FAB menu: primary FAB toggles expansion; child FABs appear above it.
                val fabExpanded = remember { mutableStateOf(false) }
                val showIncomeDialog = remember { mutableStateOf(false) }
                val manualAmountText = remember { mutableStateOf("") }
                val manualDate = remember { mutableStateOf(LocalDate.now()) }
                val manualDistribute = remember { mutableStateOf(true) }
                val manualPending = remember { mutableStateOf(false) }
                val showDatePicker = remember { mutableStateOf(false) }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (fabExpanded.value) {
                        // Record income child: pill contains icon + text and is clickable
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(40.dp)
                                    .clickable {
                                        fabExpanded.value = false
                                        showIncomeDialog.value = true
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp)) {
                                    MaterialSymbolIcon(icon = MaterialSymbols.ATTACH_MONEY, contentDescription = "Record income", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "Record income",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        // Record expense child: pill contains icon + text and is clickable
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .height(40.dp)
                                    .clickable {
                                        fabExpanded.value = false
                                        navController.navigate(SparelyDestination.ExpenseEntry.route)
                                    }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp)) {
                                    MaterialSymbolIcon(icon = MaterialSymbols.RECEIPT, contentDescription = "Record expense", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "Record expense",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Primary FAB (close/open) - fixed at 56dp
                    FloatingActionButton(onClick = { fabExpanded.value = !fabExpanded.value }, modifier = Modifier.size(56.dp)) {
                        MaterialSymbolIcon(icon = MaterialSymbols.ADD, contentDescription = "Quick actions", modifier = Modifier.size(24.dp))
                    }
                }

                // Income (paycheck) dialog re-used from previous implementation
                if (showIncomeDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showIncomeDialog.value = false },
                        title = { Text("Record income") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = manualAmountText.value,
                                    onValueChange = { v -> manualAmountText.value = v.filterCurrencyInput() },
                                    label = { Text("Amount") }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Date: ${manualDate.value.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
                                    TextButton(onClick = { showDatePicker.value = true }) { Text("Pick date") }
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Distribute to vaults")
                                    Switch(checked = manualDistribute.value, onCheckedChange = { manualDistribute.value = it })
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Create pending transfers")
                                    Switch(checked = manualPending.value, onCheckedChange = { manualPending.value = it })
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val amt = manualAmountText.value.toSafeDouble()
                                if (amt != null && amt > 0.0) {
                                    viewModel.recordPaycheck(amt, manualDate.value, manualDistribute.value, manualPending.value)
                                    manualAmountText.value = ""
                                    showIncomeDialog.value = false
                                }
                            }) { Text("Record income") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showIncomeDialog.value = false }) { Text("Cancel") }
                        }
                    )

                    if (showDatePicker.value) {
                        val initialMillis = DateUtils.toSafeDatePickerMillis(manualDate.value)
                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker.value = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val selectedMillis = datePickerState.selectedDateMillis
                                    val selected = selectedMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                                    if (selected != null) manualDate.value = selected
                                    showDatePicker.value = false
                                }) { Text("Save") }
                            },
                            dismissButton = { TextButton(onClick = { showDatePicker.value = false }) { Text("Cancel") } }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
            SparelyNavHost(
                navController = navController,
                innerPadding = innerPadding,
                viewModel = viewModel,
                uiState = uiState,
                settingsViewModel = settingsViewModel,
                settingsUiState = settingsUiState,
                vaultViewModel = vaultViewModel,
                vaultUiState = vaultUiState,
                allVaults = allVaults,
                snackbarHostState = snackbarHostState,
                onAuthenticateUser = onAuthenticateUser
            )
    }
    
    // Vault archive confirmation dialog
    uiState.vaultArchivePrompt?.let { prompt ->
    // UI observed a prompt
        VaultArchiveConfirmationDialog(
            prompt = prompt,
            onConfirmArchive = { 
                viewModel.archiveVaultFromPrompt(prompt.vaultId)
                // Navigate back to previous screen after archiving
                if (currentDestination?.route == SparelyDestination.ExpenseEntry.route) {
                    navController.popBackStack()
                }
            },
            onDismiss = {
                viewModel.dismissVaultArchivePrompt()
                // Navigate back to previous screen after dismissing
                if (currentDestination?.route == SparelyDestination.ExpenseEntry.route) {
                    navController.popBackStack()
                }
            }
        )
        // No fallback overlay; AlertDialog is the primary UI for this prompt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SparelyTopBar(currentDestination: NavDestination?, navController: NavHostController) {
    val destination = SparelyDestination.fromRoute(currentDestination?.route)
    val title = destination?.labelRes?.let { stringResource(it) } ?: stringResource(R.string.app_name)
    val isTopLevel = destination != null && destination in bottomBarDestinations
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (!isTopLevel) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
}


private val bottomBarDestinations = setOf(
    SparelyDestination.Dashboard,
    SparelyDestination.History,
    SparelyDestination.Vaults,
    SparelyDestination.Settings
)

@Composable
private fun SparelyBottomBar(
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        for (destination in bottomBarDestinations) {
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    val icon = destination.iconDrawable
                    if (icon != null) {
                        MaterialSymbolIcon(
                            icon = icon,
                            contentDescription = destination.labelRes?.let { stringResource(it) } ?: ""
                        )
                    }
                },
                label = { Text(destination.labelRes?.let { stringResource(it) } ?: "") }
            )
        }
    }
}

@Composable
private fun SparelyNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    viewModel: SparelyViewModel,
    uiState: com.example.sparely.ui.state.SparelyUiState,
    settingsViewModel: SettingsViewModel,
    settingsUiState: com.example.sparely.ui.viewmodel.SettingsUiState,
    vaultViewModel: VaultViewModel,
    vaultUiState: com.example.sparely.ui.viewmodel.VaultUiState,
    allVaults: List<com.example.sparely.domain.model.SmartVault>,
    snackbarHostState: SnackbarHostState,
    onAuthenticateUser: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = SparelyDestination.Dashboard.route,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        composable(SparelyDestination.Dashboard.route) {
            DashboardScreen(
                uiState = uiState,
                pendingVaultContributions = vaultUiState.pendingVaultContributions,
                onAddExpense = { navController.navigate(SparelyDestination.ExpenseEntry.route) },
                onRepeatLastExpense = { expense ->
                    viewModel.setPrefillExpense(expense)
                    navController.navigate(SparelyDestination.ExpenseEntry.route)
                },
                onNavigateToHistory = { navController.navigate(SparelyDestination.History.route) },
                onNavigateToBudgets = { navController.navigate(SparelyDestination.Budgets.route) },
                onNavigateToChallenges = { navController.navigate(SparelyDestination.Challenges.route) },
                onNavigateToHealth = { navController.navigate(SparelyDestination.Health.route) },
                onNavigateToRecurring = { navController.navigate(SparelyDestination.Recurring.route) },
                onManageVaults = { navController.navigate(SparelyDestination.Vaults.route) },
                onNavigateToVaultTransfers = { navController.navigate(SparelyDestination.VaultTransfers.route) },
                onNavigateToMainAccount = { navController.navigate(SparelyDestination.MainAccount.route) },
                onNavigateToCreditCards = { navController.navigate(SparelyDestination.CreditCards.route) },
                onNavigateToInsights = { navController.navigate(SparelyDestination.Insights.route) },
                // the global FAB/menu will be shown by the scaffold, so hide dashboard's own FAB
                showFloatingFab = false
            )
        }
        composable(SparelyDestination.History.route) {
            val brandSearchResults by viewModel.brandSearchResults.collectAsStateWithLifecycle()
            HistoryScreen(
                expenses = uiState.expenses,
                analytics = uiState.analytics,
                stores = uiState.stores,
                onDeleteExpense = viewModel::deleteExpense,
                onEditExpense = viewModel::updateExpense,
                onAddExpense = { navController.navigate(SparelyDestination.ExpenseEntry.route) },
                onCreateStore = { input ->
                    val storeId = viewModel.addStore(input).await()
                    viewModel.getStoreById(storeId)
                },
                onEditStore = viewModel::updateStore,
                onDeleteStore = viewModel::deleteStore,
                brandfetchClientId = uiState.settings.brandfetchClientId,
                brandSearchResults = brandSearchResults,
                onBrandSearch = viewModel::searchBrands,
                onRefundExpense = viewModel::refundExpense,
                paymentMethods = uiState.paymentMethods,
                vaults = uiState.smartVaults
            )
        }
        composable(SparelyDestination.Vaults.route) {
            VaultManagementScreen(
                vaults = allVaults,
                monthlyIncome = uiState.settings.monthlyIncome,
                recentMonthlyExpenses = uiState.analytics.totalSpent,
                savingsRate = uiState.smartSavingSummary?.actualSavingsRate ?: 0.0,
                onAddVault = vaultViewModel::addSmartVault,
                onUpdateVault = vaultViewModel::updateSmartVault,
                onDeleteVault = vaultViewModel::deleteSmartVault,
                onNavigateBack = { navController.popBackStack() },
                onManualDeposit = { vaultId, amount, reason, adjustMainAccount ->
                    vaultViewModel.depositToVault(vaultId, amount, reason, adjustMainAccount)
                },
                onManualWithdrawal = { vaultId, amount, reason, creditMainAccount ->
                    vaultViewModel.deductFromVault(vaultId, amount, reason, creditMainAccount)
                },
                onViewHistory = { vaultId ->
                    // Navigation only, history load happens in destination
                    navController.navigate(SparelyDestination.VaultHistory.createRoute(vaultId))
                },
                onBalanceOverride = { vaultId, newBalance, reason ->
                    vaultViewModel.overrideVaultBalance(vaultId, newBalance, reason)
                }
            )

        }
        composable(SparelyDestination.Budgets.route) {
            BudgetScreen(
                uiState = uiState,
                onAddBudget = viewModel::addBudget,
                onUpdateBudget = viewModel::updateBudget,
                onDeleteBudget = viewModel::deleteBudget,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.Challenges.route) {
            ChallengesScreen(
                uiState = uiState,
                onStartChallenge = viewModel::startChallenge,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.Recurring.route) {
            val brandSearchResults by viewModel.brandSearchResults.collectAsStateWithLifecycle()
            RecurringExpensesScreen(
                recurringExpenses = uiState.recurringExpenses,
                smartVaults = uiState.smartVaults,
                stores = uiState.stores,
                onAddRecurring = viewModel::addRecurringExpense,
                onUpdateRecurring = viewModel::updateRecurringExpense,
                onDeleteRecurring = viewModel::deleteRecurringExpense,
                onMarkProcessed = viewModel::markRecurringProcessed,
                onCreateStore = { input ->
                    val storeId = viewModel.addStore(input).await()
                    viewModel.getStoreById(storeId)
                },
                onEditStore = viewModel::updateStore,
                onDeleteStore = viewModel::deleteStore,
                brandfetchClientId = uiState.settings.brandfetchClientId,
                paymentMethods = uiState.paymentMethods,
                onManagePaymentMethods = { navController.navigate(SparelyDestination.Settings.route) },
                brandSearchResults = brandSearchResults,
                onBrandSearch = viewModel::searchBrands
            )
        }
        composable(SparelyDestination.Health.route) {
            FinancialHealthScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.Settings.route) {
            // Observe the result of file exports/imports to show snackbars
            LaunchedEffect(settingsUiState.errorMessage) {
                settingsUiState.errorMessage?.let { msg ->
                    if (msg.isNotEmpty()) {
                        snackbarHostState.showSnackbar(msg)
                        settingsViewModel.clearErrorMessage()
                    }
                }
            }
            
            SettingsScreen(
                settings = settingsUiState.settings,
                activeSaveRate = uiState.activeSaveRate,
                activeSavingTaxRate = uiState.activeSavingTaxRate,
                automationNotes = uiState.automationRationale,
                autoModeEnabled = settingsUiState.settings.autoRecommendationsEnabled,
                recommendation = uiState.recommendation,
                alerts = uiState.alerts,
                onPercentagesChange = settingsViewModel::updatePercentages,
                onAutoToggle = settingsViewModel::toggleAutoMode,
                onRiskChange = settingsViewModel::updateRiskLevel,
                onAgeChange = settingsViewModel::updateAge,
                onEducationStatusChange = settingsViewModel::updateEducationStatus,
                onEmploymentStatusChange = settingsViewModel::updateEmploymentStatus,
                onHasDebtsChange = settingsViewModel::updateHasDebts,
                onEmergencyFundChange = settingsViewModel::updateEmergencyFund,
                onPrimaryGoalChange = settingsViewModel::updatePrimaryGoal,
                onDisplayNameChange = settingsViewModel::updateDisplayName,
                onBirthdayChange = settingsViewModel::updateBirthday,
                onMonthlyIncomeChange = settingsViewModel::updateMonthlyIncome,
                onIncludeTaxToggle = settingsViewModel::updateIncludeTax,
                onVaultAllocationModeChange = settingsViewModel::updateVaultAllocationMode,
                onSavingTaxRateChange = settingsViewModel::updateSavingTaxRate,
                onDynamicSavingTaxToggle = settingsViewModel::updateDynamicSavingTaxEnabled,
                onReminderChange = settingsViewModel::updateReminderSettings,
                onResetHistory = settingsViewModel::resetHistory,
                onPayScheduleChange = settingsViewModel::updatePaySchedule,
                onRecordPaycheck = viewModel::recordPaycheck, // Still in SparelyViewModel for now as it involves Transactions
                onAutoDepositsEnabledChange = settingsViewModel::updateAutoDepositsEnabled,
                onAutoDepositCheckHourChange = settingsViewModel::updateAutoDepositCheckHour,
                onManualAutoDepositTrigger = settingsViewModel::triggerManualAutoDepositCheck,
                autoDepositsEnabled = settingsUiState.settings.paySchedule.autoDistributeToVaults,
                autoDepositCheckHour = settingsUiState.autoDepositCheckHour,
                onRegionalSettingsChange = settingsViewModel::updateRegionalSettings,
                onMainAccountBalanceChange = settingsViewModel::updateMainAccountBalance,
                onExportData = { uri -> settingsViewModel.exportData(uri, context) },
                onImportData = { uri ->
                    settingsViewModel.importData(uri, context) {
                        // Data refreshed automatically via Flow
                    }
                },
                expenses = uiState.expenses,
                stores = uiState.stores,
                onExportExpensesToCsv = settingsViewModel::exportExpensesToCsv,
                onExpenseHistoryRetentionChange = settingsViewModel::updateExpenseHistoryRetention,
                paymentMethods = settingsUiState.paymentMethods,
                onAddPaymentMethod = settingsViewModel::addPaymentMethod,
                onEditPaymentMethod = settingsViewModel::updatePaymentMethod,
                onDeletePaymentMethod = settingsViewModel::deletePaymentMethod,
                onCreditCardReminderChange = settingsViewModel::updateCreditCardReminderSettings,
                onCreditCardUtilizationChange = settingsViewModel::updateCreditCardUtilizationAlert,
                onBiometricEnabledChange = settingsViewModel::updateBiometricEnabled,
                onAuthenticateUser = onAuthenticateUser
            )
        }
        composable(SparelyDestination.ExpenseEntry.route) {
            val brandSearchResults by viewModel.brandSearchResults.collectAsStateWithLifecycle()
            var shouldNavigateBack by remember { mutableStateOf(false) }
            var hasVaultDeduction by remember { mutableStateOf(false) }
            
            // Handle navigation after expense is saved
            // Handle navigation after expense is saved
            LaunchedEffect(shouldNavigateBack, uiState.vaultArchivePrompt) {
                if (shouldNavigateBack) {
                    if (hasVaultDeduction) {
                        // Wait a bit for the prompt to be set if there's vault deduction
                        kotlinx.coroutines.delay(300)
                        // Navigate only if no prompt appeared
                        if (uiState.vaultArchivePrompt == null) {
                            navController.popBackStack()
                        }
                    } else {
                        // No vault deduction, navigate immediately
                        navController.popBackStack()
                    }
                }
            }
            
            ExpenseEntryScreen(
                settings = uiState.settings,
                recommendation = uiState.recommendation,
                vaults = uiState.smartVaults,
                stores = uiState.stores,
                onSave = { input ->
                    hasVaultDeduction = input.deductFromVaultId != null
                    viewModel.addExpense(input) {
                        vaultViewModel.refreshPendingContributions()
                    }
                    viewModel.clearPrefillExpense() // Clear prefill after saving
                    shouldNavigateBack = true
                },
                onCancel = { 
                    viewModel.clearPrefillExpense() // Clear prefill on cancel
                    navController.popBackStack() 
                },
                onCreateStore = { input ->
                    val storeId = viewModel.addStore(input).await()
                    viewModel.getStoreById(storeId)
                },
                onEditStore = viewModel::updateStore,
                onDeleteStore = viewModel::deleteStore,
                brandfetchClientId = uiState.settings.brandfetchClientId,
                paymentMethods = uiState.paymentMethods,
                onManagePaymentMethods = { navController.navigate(SparelyDestination.Settings.route) },
                brandSearchResults = brandSearchResults,
                onBrandSearch = viewModel::searchBrands,
                prefillExpense = uiState.prefillExpense
            )
        }
        composable(SparelyDestination.VaultTransfers.route) {
            VaultTransfersScreen(
                vaults = allVaults,
                pendingContributions = vaultUiState.pendingVaultContributions,
                onApproveContribution = vaultViewModel::approvePendingVaultContribution,
                onApproveGroup = vaultViewModel::approvePendingVaultContributions,
                onCancelContribution = vaultViewModel::cancelPendingVaultContribution,
                onStartNotificationWorkflow = vaultViewModel::startVaultTransferNotificationWorkflow,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.VaultHistory.route) { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getString("vaultId")?.toLongOrNull() ?: 0L
            
            LaunchedEffect(vaultId) {
                vaultViewModel.loadVaultHistory(vaultId)
            }
            
            val vault = allVaults.find { it.id == vaultId }
            val historyItems = vaultUiState.vaultHistory[vaultId] ?: emptyList()
            
            VaultHistoryScreen(
                vaultName = vault?.name ?: "Vault",
                historyItems = historyItems,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.MainAccount.route) {
            MainAccountScreen(
                currentBalance = uiState.settings.mainAccountBalance,
                transactions = uiState.mainAccountTransactions,
                onDeposit = viewModel::depositToMainAccount,
                onWithdraw = viewModel::withdrawFromMainAccount,
                onAdjust = viewModel::adjustMainAccountBalance,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.CreditCards.route) {
            val creditCards = uiState.paymentMethods.filter { it.isCreditCard }
            CreditCardsScreen(
                creditCards = creditCards,
                mainAccountBalance = uiState.settings.mainAccountBalance,
                recentPayments = uiState.creditCardPayments,
                onPayBill = { paymentMethodId, amount, note, deductFromMainAccount ->
                    viewModel.payCreditCardBill(paymentMethodId, amount, note, deductFromMainAccount)
                    // Optional: Refresh data or show success message if not reactive
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.Insights.route) {
            InsightsScreen(
                cashflowForecast = uiState.cashflowForecast,
                spendingPatterns = uiState.spendingPatterns,
                recurringPatterns = uiState.recurringPatterns,
                seasonalInsights = uiState.seasonalInsights,
                idleMoneyInsight = uiState.idleMoneyInsight,
                uniqueExpenses = uiState.uniqueExpenses,
                onTransferToSavings = { amount ->
                    // Navigate to vault transfers or trigger a transfer flow
                    navController.navigate(SparelyDestination.VaultTransfers.route)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private enum class SparelyDestination(
    val route: String,
    val icon: ImageVector?,
    val iconDrawable: Int?,
    @StringRes val labelRes: Int?
) {
    Dashboard("dashboard", null, MaterialSymbols.HOME, R.string.dashboard_title),
    History("history", null, MaterialSymbols.BAR_CHART, R.string.history_title),
    Vaults("vaults", null, MaterialSymbols.ACCOUNT_BALANCE_WALLET, R.string.vaults_title),
    Budgets("budgets", null, MaterialSymbols.ACCOUNT_BALANCE, R.string.budgets_title),
    Challenges("challenges", null, MaterialSymbols.TROPHY, R.string.challenges_title),
    Recurring("recurring", null, MaterialSymbols.SCHEDULE, R.string.recurring_title),
    Health("health", null, MaterialSymbols.FAVORITE, R.string.health_title),
    Settings("settings", null, MaterialSymbols.SETTINGS, R.string.settings_title),
    ExpenseEntry("expense", null, MaterialSymbols.SAVINGS, R.string.expense_entry_title),
    CreditCards("creditCards", null, MaterialSymbols.CREDIT_CARD, R.string.dashboard_credit_cards_title),
    VaultTransfers("vaultTransfers", null, MaterialSymbols.SWAP_HORIZ, R.string.vault_transfers_title),
    VaultHistory("vaultHistory/{vaultId}", null, MaterialSymbols.HISTORY, R.string.vault_history_screen_title),

    MainAccount("mainAccount", null, MaterialSymbols.ACCOUNT_BALANCE_WALLET, R.string.dashboard_main_account_title),
    Insights("insights", null, MaterialSymbols.LIGHTBULB, R.string.insights_title);

    fun createRoute(vararg args: Any): String {
        var result = route
        args.forEach { arg ->
            result = result.replaceFirst(Regex("\\{[^}]+\\}"), arg.toString())
        }
        return result
    }

    companion object {
        fun fromRoute(route: String?): SparelyDestination? {
            return entries.find { it.route == route }
        }
    }
}

@Composable
private fun VaultArchiveConfirmationDialog(
    prompt: VaultArchivePrompt,
    onConfirmArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    // dialog composed for vault archive prompt
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Archive ${prompt.vaultName}?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You've used ${String.format("%.0f%%", (prompt.expenseAmount / prompt.vaultBalanceBefore.coerceAtLeast(0.01)) * 100)} of this vault's balance.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Expense amount: $${String.format("%.2f", prompt.expenseAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vault balance before: $${String.format("%.2f", prompt.vaultBalanceBefore)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (prompt.overflowToMainAccount > 0) {
                        Text(
                            text = "Overflow to main account: $${String.format("%.2f", prompt.overflowToMainAccount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = "Would you like to archive this vault now that it's nearly depleted?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = { 
            TextButton(onClick = { onConfirmArchive() }) {
                Text("Archive Vault")
            }
        },
        dismissButton = { 
            TextButton(onClick = { onDismiss() }) {
                Text("Keep Active")
            }
        }
    )
}
