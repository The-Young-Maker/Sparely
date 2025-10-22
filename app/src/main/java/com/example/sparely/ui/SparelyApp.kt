package com.example.sparely.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sparely.ui.screens.BudgetScreen
import com.example.sparely.ui.screens.ChallengesScreen
import com.example.sparely.ui.screens.DashboardScreen
import com.example.sparely.ui.screens.ExpenseEntryScreen
import com.example.sparely.ui.screens.FinancialHealthScreen
import com.example.sparely.ui.screens.GoalsScreen
import com.example.sparely.ui.screens.HistoryScreen
import com.example.sparely.ui.screens.OnboardingScreen
import com.example.sparely.ui.screens.RecurringExpensesScreen
import com.example.sparely.ui.screens.VaultManagementScreen
import com.example.sparely.ui.screens.VaultTransfersScreen
import com.example.sparely.ui.screens.SettingsScreen
import com.example.sparely.ui.screens.VaultTransfersScreen

@Composable
fun SparelyApp(viewModel: SparelyViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
    SparelyDestination.Goals,
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
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = destination.label)
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
                onConfirmSmartTransfer = viewModel::confirmSmartTransfer,
                onSnoozeSmartTransfer = viewModel::snoozeSmartTransfer,
                onDismissSmartTransfer = viewModel::dismissSmartTransfer,
                onCompleteSmartTransfer = viewModel::completeSmartTransfer,
                onCancelSmartTransfer = viewModel::cancelSmartTransfer,
                onManageVaults = { navController.navigate("vaultManagement") },
                onNavigateToVaultTransfers = { navController.navigate("vaultTransfers") }
            )
        }
        composable(SparelyDestination.History.route) {
            HistoryScreen(
                expenses = uiState.expenses,
                analytics = uiState.analytics,
                onDeleteExpense = viewModel::deleteExpense
            )
        }
        composable(SparelyDestination.Goals.route) {
            GoalsScreen(
                goals = uiState.goals,
                recommendation = uiState.recommendation,
                onAddGoal = viewModel::addGoal,
                onArchiveToggle = viewModel::toggleGoalArchived,
                onDeleteGoal = viewModel::deleteGoal
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
                savingsPlan = uiState.savingsPlan,
                manualTransfers = uiState.manualTransfers,
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
                onLogTransfer = viewModel::logManualTransfer,
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
                autoDepositCheckHour = uiState.autoDepositCheckHour
            )
        }
        composable(SparelyDestination.ExpenseEntry.route) {
            ExpenseEntryScreen(
                settings = uiState.settings,
                recommendation = uiState.recommendation,
                onSave = {
                    viewModel.addExpense(it)
                    navController.popBackStack()
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
        composable("vaultManagement") {
            VaultManagementScreen(
                vaults = uiState.smartVaults,
                onAddVault = viewModel::addSmartVault,
                onUpdateVault = viewModel::updateSmartVault,
                onDeleteVault = viewModel::deleteSmartVault,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private enum class SparelyDestination(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Dashboard("dashboard", Icons.Default.Home, "Dashboard"),
    History("history", Icons.Default.BarChart, "History"),
    Goals("goals", Icons.Default.Save, "Goals"),
    Budgets("budgets", Icons.Default.AccountBalance, "Budgets"),
    Challenges("challenges", Icons.Default.EmojiEvents, "Challenges"),
    Recurring("recurring", Icons.Default.Schedule, "Recurring"),
    Health("health", Icons.Default.Favorite, "Health"),
    Settings("settings", Icons.Default.MoreHoriz, "Settings"),
    ExpenseEntry("expense", Icons.Default.Save, "Log purchase");

    companion object {
        fun fromRoute(route: String?): SparelyDestination? {
            return entries.find { it.route == route }
        }
    }
}
