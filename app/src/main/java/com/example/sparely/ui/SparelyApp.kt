package com.example.sparely.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.sparely.ui.theme.MaterialSymbolIcon
import com.example.sparely.ui.theme.MaterialSymbols

@Composable
fun SparelyApp(
    viewModel: SparelyViewModel,
    deepLinkDestination: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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

    if (!uiState.onboardingCompleted) {
        OnboardingScreen(
            onComplete = { profile -> viewModel.completeOnboarding(profile) },
            onSkip = viewModel::skipOnboarding
        )
    } else {
        SparelyScaffold(
            navController = navController,
            snackbarHostState = snackbarHostState,
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SparelyScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    uiState: com.example.sparely.ui.state.SparelyUiState,
    viewModel: SparelyViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showFab = currentDestination?.route != SparelyDestination.ExpenseEntry.route
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SparelyTopBar(currentDestination, navController)
        },
        bottomBar = {
            SparelyBottomBar(
                currentDestination = currentDestination,
                navController = navController
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = {
                    navController.navigate(SparelyDestination.ExpenseEntry.route)
                }) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Log purchase")
                }
            }
        }
    ) { innerPadding ->
        SparelyNavHost(
            navController = navController,
            innerPadding = innerPadding,
            viewModel = viewModel,
            uiState = uiState
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
    val title = destination?.label ?: "Sparely"
    val isTopLevel = destination == null || destination in bottomBarDestinations
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (!isTopLevel) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
    NavigationBar {
        bottomBarDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    val targetRoute = destination.route
                    if (selected) {
                        navController.popBackStack(targetRoute, inclusive = false)
                    } else {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (destination.iconDrawable != null) {
                        MaterialSymbolIcon(
                            icon = destination.iconDrawable,
                            contentDescription = destination.label,
                            size = 24.dp
                        )
                    } else if (destination.icon != null) {
                        Icon(imageVector = destination.icon, contentDescription = destination.label)
                    }
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun SparelyNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    viewModel: SparelyViewModel,
    uiState: com.example.sparely.ui.state.SparelyUiState
) {
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
                onAddExpense = { navController.navigate(SparelyDestination.ExpenseEntry.route) },
                onNavigateToHistory = { navController.navigate(SparelyDestination.History.route) },
                onNavigateToBudgets = { navController.navigate(SparelyDestination.Budgets.route) },
                onNavigateToChallenges = { navController.navigate(SparelyDestination.Challenges.route) },
                onNavigateToHealth = { navController.navigate(SparelyDestination.Health.route) },
                onNavigateToRecurring = { navController.navigate(SparelyDestination.Recurring.route) },
                onManageVaults = { navController.navigate(SparelyDestination.Vaults.route) },
                onNavigateToVaultTransfers = { navController.navigate("vaultTransfers") },
                onNavigateToMainAccount = { navController.navigate("mainAccount") }
            )
        }
        composable(SparelyDestination.History.route) {
            HistoryScreen(
                expenses = uiState.expenses,
                analytics = uiState.analytics,
                onDeleteExpense = viewModel::deleteExpense
            )
        }
        composable(SparelyDestination.Vaults.route) {
            VaultManagementScreen(
                vaults = uiState.smartVaults,
                onAddVault = viewModel::addSmartVault,
                onUpdateVault = viewModel::updateSmartVault,
                onDeleteVault = viewModel::deleteSmartVault,
                onNavigateBack = { navController.popBackStack() },
                onManualDeposit = { vaultId, amount, reason ->
                    viewModel.depositToVault(vaultId, amount, reason)
                },
                onManualWithdrawal = { vaultId, amount, reason ->
                    viewModel.deductFromVault(vaultId, amount, reason)
                },
                onViewHistory = { vaultId ->
                    viewModel.loadVaultAdjustmentHistory(vaultId)
                    navController.navigate("vaultHistory/$vaultId")
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
            RecurringExpensesScreen(
                recurringExpenses = uiState.recurringExpenses,
                smartVaults = uiState.smartVaults,
                onAddRecurring = viewModel::addRecurringExpense,
                onUpdateRecurring = viewModel::updateRecurringExpense,
                onDeleteRecurring = viewModel::deleteRecurringExpense,
                onMarkProcessed = viewModel::markRecurringProcessed
            )
        }
        composable(SparelyDestination.Health.route) {
            FinancialHealthScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SparelyDestination.Settings.route) {
            SettingsScreen(
                settings = uiState.settings,
                activeSaveRate = uiState.activeSaveRate,
                activeSavingTaxRate = uiState.activeSavingTaxRate,
                automationNotes = uiState.automationRationale,
                autoModeEnabled = uiState.settings.autoRecommendationsEnabled,
                recommendation = uiState.recommendation,
                alerts = uiState.alerts,
                onPercentagesChange = viewModel::updatePercentages,
                onAutoToggle = viewModel::toggleAutoMode,
                onRiskChange = viewModel::updateRiskLevel,
                onAgeChange = viewModel::updateAge,
                onEducationStatusChange = viewModel::updateEducationStatus,
                onEmploymentStatusChange = viewModel::updateEmploymentStatus,
                onHasDebtsChange = viewModel::updateHasDebts,
                onEmergencyFundChange = viewModel::updateEmergencyFund,
                onPrimaryGoalChange = viewModel::updatePrimaryGoal,
                onDisplayNameChange = viewModel::updateDisplayName,
                onBirthdayChange = viewModel::updateBirthday,
                onMonthlyIncomeChange = viewModel::updateMonthlyIncome,
                onIncludeTaxToggle = viewModel::updateIncludeTax,
                onVaultAllocationModeChange = viewModel::updateVaultAllocationMode,
                onSavingTaxRateChange = viewModel::updateSavingTaxRate,
                onDynamicSavingTaxToggle = viewModel::updateDynamicSavingTaxEnabled,
                onReminderChange = viewModel::updateReminderSettings,
                onResetHistory = viewModel::resetHistory,
                onPayScheduleChange = viewModel::updatePaySchedule,
                onRecordPaycheck = viewModel::recordPaycheck,
                onAutoDepositsEnabledChange = viewModel::updateAutoDepositsEnabled,
                onAutoDepositCheckHourChange = viewModel::updateAutoDepositCheckHour,
                onManualAutoDepositTrigger = viewModel::triggerManualAutoDepositCheck,
                autoDepositsEnabled = uiState.settings.paySchedule.autoDistributeToVaults,
                autoDepositCheckHour = uiState.autoDepositCheckHour,
                onRegionalSettingsChange = viewModel::updateRegionalSettings,
                onMainAccountBalanceChange = viewModel::updateMainAccountBalance
            )
        }
        composable(SparelyDestination.ExpenseEntry.route) {
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
                onSave = { input ->
                    hasVaultDeduction = input.deductFromVaultId != null
                    viewModel.addExpense(input)
                    shouldNavigateBack = true
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable("vaultTransfers") {
            VaultTransfersScreen(
                vaults = uiState.smartVaults,
                pendingContributions = uiState.pendingVaultContributions,
                onReconcileContribution = viewModel::reconcileVaultContribution,
                onReconcileGroup = viewModel::reconcileVaultContributions,
                onStartNotificationWorkflow = viewModel::startVaultTransferNotificationWorkflow,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("vaultHistory/{vaultId}") { backStackEntry ->
            val vaultId = backStackEntry.arguments?.getString("vaultId")?.toLongOrNull() ?: 0L
            val vault = uiState.smartVaults.find { it.id == vaultId }
            val adjustments = uiState.vaultAdjustments[vaultId] ?: emptyList()
            
            VaultHistoryScreen(
                vaultName = vault?.name ?: "Vault",
                adjustments = adjustments,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("mainAccount") {
            MainAccountScreen(
                currentBalance = uiState.settings.mainAccountBalance,
                transactions = uiState.mainAccountTransactions,
                onDeposit = viewModel::depositToMainAccount,
                onWithdraw = viewModel::withdrawFromMainAccount,
                onAdjust = viewModel::adjustMainAccountBalance,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private enum class SparelyDestination(
    val route: String,
    val icon: ImageVector?,
    val iconDrawable: Int?,
    val label: String
) {
    Dashboard("dashboard", null, MaterialSymbols.HOME, "Dashboard"),
    History("history", null, MaterialSymbols.BAR_CHART, "History"),
    Vaults("vaults", null, MaterialSymbols.ACCOUNT_BALANCE_WALLET, "Vaults"),
    Budgets("budgets", null, MaterialSymbols.ACCOUNT_BALANCE, "Budgets"),
    Challenges("challenges", null, MaterialSymbols.TROPHY, "Challenges"),
    Recurring("recurring", null, MaterialSymbols.SCHEDULE, "Recurring"),
    Health("health", null, MaterialSymbols.FAVORITE, "Health"),
    Settings("settings", null, MaterialSymbols.SETTINGS, "Settings"),
    ExpenseEntry("expense", null, MaterialSymbols.SAVINGS, "Log purchase");

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
