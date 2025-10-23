package com.example.sparely.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.logic.SavingsAdvisor
import com.example.sparely.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (UserProfileSetup) -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var selectedCountry by remember { mutableStateOf<CountryConfig?>(null) }
    var userName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("30") }
    var birthday by remember { mutableStateOf<LocalDate?>(null) }
    var monthlyIncome by remember { mutableStateOf("") }
    var selectedRiskLevel by remember { mutableStateOf(RiskLevel.BALANCED) }
    var primaryGoal by remember { mutableStateOf("") }
    var hasDebts by remember { mutableStateOf(false) }
    var currentEmergencyFund by remember { mutableStateOf("0") }
    var educationStatus by remember { mutableStateOf(EducationStatus.OTHER) }
    var employmentStatus by remember { mutableStateOf(EmploymentStatus.EMPLOYED) }
    var livingSituation by remember { mutableStateOf(LivingSituation.OTHER) }
    var occupation by remember { mutableStateOf("") }
    var mainAccountBalance by remember { mutableStateOf("") }
    var savingsAccountBalance by remember { mutableStateOf("") }
    var vaultsBalanceInput by remember { mutableStateOf("") }
    val vaultDrafts = remember { mutableStateListOf<VaultDraft>() }
    var nextDraftId by remember { mutableStateOf(0L) }
    val subscriptionDrafts = remember { mutableStateListOf<SubscriptionDraft>() }
    var nextSubscriptionId by remember { mutableStateOf(0L) }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderFrequency by remember { mutableStateOf(7) }
    var reminderHour by remember { mutableStateOf(20) }

    val totalSteps = 8  // Increased from 7 to 8
    val vaultsStepIndex = 6  // Shifted from 5 to 6
    val derivedAge = birthday?.let { ChronoUnit.YEARS.between(it, LocalDate.now()).coerceAtLeast(0L).toInt() }
    val ageValue = derivedAge ?: age.toIntOrNull() ?: 30
    val monthlyIncomeValue = monthlyIncome.toDoubleOrNull() ?: 0.0
    val emergencyFundValue = currentEmergencyFund.toDoubleOrNull() ?: 0.0

    fun allocateDraftId(): Long {
        val id = nextDraftId
        nextDraftId += 1
        return id
    }

    fun removeVaultAt(index: Int) {
        if (index < 0 || index >= vaultDrafts.size) return
        vaultDrafts.removeAt(index)
    }

    fun addVaultDraft(template: VaultDraft = VaultDraft.blank()) {
        val newDraft = template.copy(id = allocateDraftId())
        vaultDrafts.add(newDraft)
    }

    fun updateVaultDraft(index: Int, updated: VaultDraft) {
        if (index < 0 || index >= vaultDrafts.size) return
        vaultDrafts[index] = updated
    }

    fun allocateSubscriptionId(): Long {
        val id = nextSubscriptionId
        nextSubscriptionId += 1
        return id
    }

    fun addSubscriptionDraft() {
        subscriptionDrafts.add(SubscriptionDraft(id = allocateSubscriptionId(), name = "", amount = ""))
    }

    fun updateSubscriptionName(id: Long, value: String) {
        val index = subscriptionDrafts.indexOfFirst { it.id == id }
        if (index >= 0) {
            val draft = subscriptionDrafts[index]
            subscriptionDrafts[index] = draft.copy(name = value)
        }
    }

    fun updateSubscriptionAmount(id: Long, value: String) {
        val index = subscriptionDrafts.indexOfFirst { it.id == id }
        if (index >= 0) {
            val draft = subscriptionDrafts[index]
            subscriptionDrafts[index] = draft.copy(amount = value)
        }
    }

    fun removeSubscription(id: Long) {
        subscriptionDrafts.removeAll { it.id == id }
    }

    LaunchedEffect(
        currentStep,
        ageValue,
        educationStatus,
        employmentStatus,
        hasDebts,
        monthlyIncome,
        currentEmergencyFund
    ) {
        if (currentStep == vaultsStepIndex && vaultDrafts.isEmpty()) {
            val templates = recommendedVaultDrafts(
                age = ageValue,
                educationStatus = educationStatus,
                employmentStatus = employmentStatus,
                hasDebts = hasDebts,
                monthlyIncome = monthlyIncomeValue,
                currentEmergencyFund = emergencyFundValue
            )
            templates.forEach { template ->
                addVaultDraft(template)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Progress indicator (skip on country selection step)
            if (currentStep > 0) {
                OnboardingProgressBar(
                    currentStep = currentStep - 1,  // Adjust for zero-indexed country step
                    totalSteps = totalSteps - 1,
                    onBack = { if (currentStep > 0) currentStep-- }
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    },
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        0 -> CountrySelectionStep(
                            selectedCountry = selectedCountry,
                            onCountrySelected = { selectedCountry = it },
                            onNext = { currentStep = 1 }
                        )
                        1 -> WelcomeStep(
                            countryConfig = selectedCountry,
                            onNext = { currentStep = 2 },
                            onSkip = onSkip
                        )
                        2 -> NameStep(
                            name = userName,
                            onNameChange = { userName = it },
                            onNext = { currentStep = 3 }
                        )
                        3 -> IncomeStep(
                            income = monthlyIncome,
                            age = age,
                            birthday = birthday,
                            onIncomeChange = { monthlyIncome = it },
                            onAgeChange = {
                                age = it
                                if (birthday != null) {
                                    birthday = null
                                }
                            },
                            onBirthdayChange = { selected ->
                                birthday = selected
                                selected?.let {
                                    val computedAge = ChronoUnit.YEARS.between(it, LocalDate.now()).coerceAtLeast(0L).toInt()
                                    age = computedAge.toString()
                                }
                            },
                            onNext = { currentStep = 4 }
                        )
                        4 -> RiskLevelStep(
                            selectedRisk = selectedRiskLevel,
                            onRiskSelected = { selectedRiskLevel = it },
                            onNext = { currentStep = 5 }
                        )
                        5 -> FinancialSituationStep(
                            hasDebts = hasDebts,
                            onDebtsChange = { hasDebts = it },
                            emergencyFund = currentEmergencyFund,
                            onEmergencyFundChange = { currentEmergencyFund = it },
                            educationStatus = educationStatus,
                            onEducationStatusChange = { educationStatus = it },
                            employmentStatus = employmentStatus,
                            onEmploymentStatusChange = { employmentStatus = it },
                            livingSituation = livingSituation,
                            onLivingSituationChange = { livingSituation = it },
                            occupation = occupation,
                            onOccupationChange = { occupation = it },
                            mainAccountBalance = mainAccountBalance,
                            onMainAccountBalanceChange = { mainAccountBalance = it },
                            savingsAccountBalance = savingsAccountBalance,
                            onSavingsAccountBalanceChange = { savingsAccountBalance = it },
                            vaultsBalance = vaultsBalanceInput,
                            onVaultsBalanceChange = { vaultsBalanceInput = it },
                            subscriptions = subscriptionDrafts,
                            onAddSubscription = { addSubscriptionDraft() },
                            onSubscriptionNameChange = { id, value -> updateSubscriptionName(id, value) },
                            onSubscriptionAmountChange = { id, value -> updateSubscriptionAmount(id, value) },
                            onRemoveSubscription = { id -> removeSubscription(id) },
                            age = ageValue,
                            onNext = { currentStep = 6 }
                        )
                        6 -> SmartVaultsStep(
                            drafts = vaultDrafts,
                            onDraftChange = { index, draft -> updateVaultDraft(index, draft) },
                            onRemove = { index -> removeVaultAt(index) },
                            onAddVault = { addVaultDraft() },
                            onNext = { currentStep = 7 }
                        )
                        7 -> TransferReminderStep(
                            reminderEnabled = reminderEnabled,
                            onReminderEnabledChange = { reminderEnabled = it },
                            reminderFrequency = reminderFrequency,
                            onReminderFrequencyChange = { reminderFrequency = it },
                            reminderHour = reminderHour,
                            onReminderHourChange = { reminderHour = it },
                            pendingVaults = vaultDrafts,
                            onNext = { currentStep = 8 }
                        )
                        8 -> GoalStep(
                            goal = primaryGoal,
                            onGoalChange = { primaryGoal = it },
                            onComplete = {
                                val vaults = vaultDrafts.mapNotNull { draft -> draft.toSetup() }
                                val reminderPreference = TransferReminderPreference(
                                    enabled = reminderEnabled,
                                    frequencyDays = reminderFrequency,
                                    hourOfDay = reminderHour
                                )
                                val sanitizedOccupation = occupation.trim().ifBlank { null }
                                val mainBalance = mainAccountBalance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                val savingsBalance = savingsAccountBalance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                val vaultsBalance = vaultsBalanceInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                val subscriptions = subscriptionDrafts.mapNotNull { draft ->
                                    val name = draft.name.trim()
                                    val amount = draft.amount.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                    if (name.isEmpty() || amount <= 0.0) {
                                        null
                                    } else {
                                        OnboardingSubscription(name = name, amount = amount)
                                    }
                                }
                                val profile = UserProfileSetup(
                                    name = userName.ifBlank { null },
                                    age = age.toIntOrNull() ?: 30,
                                    monthlyIncome = monthlyIncome.toDoubleOrNull() ?: 4500.0,
                                    riskLevel = selectedRiskLevel,
                                    hasDebts = hasDebts,
                                    currentEmergencyFund = currentEmergencyFund.toDoubleOrNull() ?: 0.0,
                                    primaryGoal = primaryGoal.ifBlank { null },
                                    smartVaults = vaults,
                                    transferReminder = reminderPreference,
                                    educationStatus = educationStatus,
                                    employmentStatus = employmentStatus,
                                    livingSituation = livingSituation,
                                    occupation = sanitizedOccupation,
                                    mainAccountBalance = mainBalance,
                                    savingsAccountBalance = savingsBalance,
                                    vaultsBalance = vaultsBalance,
                                    subscriptions = subscriptions,
                                    birthday = birthday
                                )
                                onComplete(profile)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
        progress = { currentStep.toFloat() / totalSteps },
        modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
    }
}

@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💰",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 2
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to Sparely",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your intelligent savings companion that helps you automatically set aside money from every purchase",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureHighlight(
                icon = "🎯",
                title = "Smart Allocation",
                description = "Automatically split expenses into Emergency, Investment, and Fun funds"
            )
            
            FeatureHighlight(
                icon = "📊",
                title = "Financial Health Score",
                description = "Track your financial wellness with personalized insights"
            )
            
            FeatureHighlight(
                icon = "🏆",
                title = "Savings Challenges",
                description = "Gamify your savings with fun challenges and achievements"
            )
            
            FeatureHighlight(
                icon = "💡",
                title = "Smart Budgeting",
                description = "Set budgets and get alerts before overspending"
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onSkip) {
            Text("Skip Setup")
        }
    }
}

@Composable
fun FeatureHighlight(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransferReminderStep(
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderFrequency: Int,
    onReminderFrequencyChange: (Int) -> Unit,
    reminderHour: Int,
    onReminderHourChange: (Int) -> Unit,
    pendingVaults: List<VaultDraft>,
    onNext: () -> Unit
) {
    val reminderOptions = listOf(3, 7, 14, 30)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Stay on top of your vault funding",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Sparely can nudge you to actually move the cash into the vault destinations you just mapped. Contributions only count after you confirm they happened.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = reminderEnabled, onCheckedChange = onReminderEnabledChange)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Enable transfer reminders")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "How often should we remind you?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reminderOptions.forEach { option ->
                FilterChip(
                    selected = reminderFrequency == option,
                    onClick = { onReminderFrequencyChange(option) },
                    enabled = reminderEnabled,
                    label = { Text("Every $option days") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Preferred reminder time",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = reminderHour.toFloat(),
            onValueChange = { value -> onReminderHourChange(value.roundToInt().coerceIn(0, 23)) },
            valueRange = 0f..23f,
            steps = 22,
            enabled = reminderEnabled
        )
        Text(
            text = "Remind me around ${reminderHour.toString().padStart(2, '0')}:00",
            style = MaterialTheme.typography.bodyMedium
        )

        if (pendingVaults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "We will match reminders to: ${pendingVaults.joinToString { it.name.ifBlank { "Vault" } }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SmartVaultsStep(
        drafts: List<VaultDraft>,
        onDraftChange: (Int, VaultDraft) -> Unit,
        onRemove: (Int) -> Unit,
        onAddVault: () -> Unit,
        onNext: () -> Unit
    ) {
        val canProceed = drafts.isNotEmpty() && drafts.all { draft ->
            draft.name.isNotBlank() && draft.targetAmount.toDoubleOrNull()?.let { it > 0.0 } == true
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Design your smart vaults",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Vaults are the destinations Sparely funds automatically. We use them to calculate priorities, savings tax boosts, and auto deposits.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            drafts.forEachIndexed { index, draft ->
                SmartVaultCard(
                    draft = draft,
                    onDraftChange = { updated -> onDraftChange(index, updated) },
                    onRemove = { onRemove(index) },
                    showRemove = drafts.size > 1
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedButton(onClick = onAddVault) {
                Text("Add another vault")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SmartVaultCard(
        draft: VaultDraft,
        onDraftChange: (VaultDraft) -> Unit,
        onRemove: () -> Unit,
        showRemove: Boolean
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (draft.recommended) {
                    AssistChip(onClick = {}, enabled = false, label = { Text("Recommended") })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    label = { Text("Vault name") },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draft.targetAmount,
                        onValueChange = { onDraftChange(draft.copy(targetAmount = it)) },
                        label = { Text("Target amount") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = draft.currentBalance,
                        onValueChange = { onDraftChange(draft.copy(currentBalance = it)) },
                        label = { Text("Current balance") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = draft.priority == priority,
                            onClick = { onDraftChange(draft.copy(priority = priority)) },
                            label = { Text(priority.displayName()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Vault focus",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultType.entries.forEach { type ->
                        FilterChip(
                            selected = draft.type == type,
                            onClick = { onDraftChange(draft.copy(type = type)) },
                            label = { Text(type.displayName()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Allocation mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.allocationMode == VaultAllocationMode.DYNAMIC_AUTO,
                        onClick = {
                            onDraftChange(draft.copy(allocationMode = VaultAllocationMode.DYNAMIC_AUTO, manualPercent = ""))
                        },
                        label = { Text("Dynamic") }
                    )
                    FilterChip(
                        selected = draft.allocationMode == VaultAllocationMode.MANUAL,
                        onClick = {
                            onDraftChange(draft.copy(allocationMode = VaultAllocationMode.MANUAL))
                        },
                        label = { Text("Manual") }
                    )
                }

                if (draft.allocationMode == VaultAllocationMode.MANUAL) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft.manualPercent,
                        onValueChange = { onDraftChange(draft.copy(manualPercent = it)) },
                        label = { Text("Manual allocation %") },
                        placeholder = { Text("e.g. 25") },
                        trailingIcon = { Text("%") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = draft.savingTaxRate,
                    onValueChange = { onDraftChange(draft.copy(savingTaxRate = it)) },
                    label = { Text("Saving tax boost % (optional)") },
                    placeholder = { Text("e.g. 5") },
                    trailingIcon = { Text("%") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showRemove) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onRemove) {
                        Text("Remove vault")
                    }
                }
            }
        }
    }

    private data class VaultDraft(
        val id: Long,
        val name: String,
        val targetAmount: String,
        val currentBalance: String,
        val priority: VaultPriority,
        val type: VaultType,
        val allocationMode: VaultAllocationMode,
        val manualPercent: String,
        val savingTaxRate: String,
        val recommended: Boolean
    ) {
        companion object {
            fun blank(): VaultDraft = VaultDraft(
                id = -1,
                name = "New vault",
                targetAmount = "2000",
                currentBalance = "0",
                priority = VaultPriority.MEDIUM,
                type = VaultType.SHORT_TERM,
                allocationMode = VaultAllocationMode.DYNAMIC_AUTO,
                manualPercent = "",
                savingTaxRate = "",
                recommended = false
            )
        }

        fun toSetup(): SmartVaultSetup? {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return null
            val target = targetAmount.toDoubleOrNull()?.coerceAtLeast(0.0) ?: return null
            if (target <= 0.0) return null
            val balance = currentBalance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            val manualShare = if (allocationMode == VaultAllocationMode.MANUAL) {
                manualPercent.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0)
            } else {
                null
            }
            val taxOverride = savingTaxRate.toDoubleOrNull()?.div(100.0)?.coerceIn(0.0, 1.0)
            return SmartVaultSetup(
                name = trimmedName,
                targetAmount = target,
                currentBalance = balance,
                priority = priority,
                type = type,
                allocationMode = allocationMode,
                manualAllocationPercent = manualShare,
                savingTaxRateOverride = taxOverride
            )
        }
    }

    private fun recommendedVaultDrafts(
        age: Int,
        educationStatus: EducationStatus,
        employmentStatus: EmploymentStatus,
        hasDebts: Boolean,
        monthlyIncome: Double,
        currentEmergencyFund: Double
    ): List<VaultDraft> {
        val setups = SavingsAdvisor.recommendedVaults(
            age = age,
            educationStatus = educationStatus,
            employmentStatus = employmentStatus,
            hasDebts = hasDebts,
            emergencyFund = currentEmergencyFund,
            monthlyIncome = monthlyIncome
        )
        return setups.map { it.toDraft(recommended = true) }
    }

    private data class SubscriptionDraft(
        val id: Long,
        val name: String,
        val amount: String
    )

    private fun SmartVaultSetup.toDraft(recommended: Boolean): VaultDraft = VaultDraft(
        id = -1,
        name = name,
        targetAmount = targetAmount.toInputText(defaultWhenZero = ""),
        currentBalance = currentBalance.toInputText(defaultWhenZero = "0"),
        priority = priority,
        type = type,
        allocationMode = allocationMode,
        manualPercent = manualAllocationPercent?.let { (it * 100).toPercentageInput() } ?: "",
        savingTaxRate = savingTaxRateOverride?.let { (it * 100).toPercentageInput() } ?: "",
        recommended = recommended
    )

private fun EducationStatus.displayName(): String = when (this) {
    EducationStatus.HIGH_SCHOOL -> "High School"
    EducationStatus.UNIVERSITY -> "University/College"
    EducationStatus.GRADUATED -> "Graduated"
    EducationStatus.OTHER -> "Other"
}

private fun EmploymentStatus.displayName(): String = when (this) {
    EmploymentStatus.STUDENT -> "Student"
    EmploymentStatus.PART_TIME -> "Part-time"
    EmploymentStatus.FULL_TIME, EmploymentStatus.EMPLOYED -> "Full-time"
    EmploymentStatus.SELF_EMPLOYED -> "Self-employed"
    EmploymentStatus.UNEMPLOYED -> "Unemployed"
    EmploymentStatus.RETIRED -> "Retired"
}

private fun LivingSituation.displayName(): String = when (this) {
    LivingSituation.WITH_PARENTS -> "With family"
    LivingSituation.RENTING -> "Renting"
    LivingSituation.HOMEOWNER -> "Homeowner"
    LivingSituation.OTHER -> "Other"
}

private fun VaultPriority.displayName(): String = when (this) {
    VaultPriority.LOW -> "Low"
    VaultPriority.MEDIUM -> "Medium"
    VaultPriority.HIGH -> "High"
    VaultPriority.CRITICAL -> "Critical"
}

private fun VaultType.displayName(): String = when (this) {
    VaultType.SHORT_TERM -> "Short-term"
    VaultType.LONG_TERM -> "Long-term"
    VaultType.PASSIVE_INVESTMENT -> "Passive"
}

private fun Double.toInputText(defaultWhenZero: String = ""): String {
    if (this <= 0.0) return defaultWhenZero
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", this)
    }
}

private fun Double.toPercentageInput(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
}

@Composable
fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "👋",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "This helps us personalize your experience",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your name (optional)") },
            placeholder = { Text("e.g., Alex") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeStep(
    income: String,
    age: String,
    birthday: LocalDate?,
    onIncomeChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onBirthdayChange: (LocalDate?) -> Unit,
    onNext: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val computedAge = remember(birthday) {
        birthday?.let { ChronoUnit.YEARS.between(it, LocalDate.now()).coerceAtLeast(0L).toInt() }
    }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    val birthdayLabel = birthday?.format(dateFormatter) ?: "Add your birthday (optional)"
    val isAgeValid = (birthday != null && computedAge != null) || age.toIntOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "💵",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Let's understand your finances",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "This helps us provide personalized savings recommendations",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedTextField(
            value = income,
            onValueChange = onIncomeChange,
            label = { Text("Monthly Income") },
            placeholder = { Text("4500") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.AttachMoney, contentDescription = null)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showBirthdayPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(birthdayLabel)
        }

        if (birthday != null && computedAge != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We'll keep track that you're $computedAge and tailor milestones accordingly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = { onBirthdayChange(null) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear birthday")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val ageHelper = if (birthday != null) {
            "Calculated automatically from your birthday."
        } else {
            "You can adjust this later in settings."
        }

        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Your Age") },
            placeholder = { Text("30") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = birthday == null,
            leadingIcon = {
                Icon(Icons.Default.Cake, contentDescription = null)
            },
            supportingText = { Text(ageHelper) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your data stays private on your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = income.toDoubleOrNull() != null && isAgeValid
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showBirthdayPicker) {
        val initialMillis = birthday?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        LaunchedEffect(initialMillis) {
            if (initialMillis != null && datePickerState.selectedDateMillis != initialMillis) {
                datePickerState.selectedDateMillis = initialMillis
            }
        }
        DatePickerDialog(
            onDismissRequest = { showBirthdayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    val selectedDate = selectedMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    onBirthdayChange(selectedDate)
                    showBirthdayPicker = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthdayPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun RiskLevelStep(
    selectedRisk: RiskLevel,
    onRiskSelected: (RiskLevel) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "📈",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "What's your investment style?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "This affects how we split your investment allocations",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        RiskLevelOption(
            icon = "🛡️",
            title = "Conservative",
            description = "Focus on safety with 80%+ in stable investments (bonds, ETFs)",
            isSelected = selectedRisk == RiskLevel.CONSERVATIVE,
            onClick = { onRiskSelected(RiskLevel.CONSERVATIVE) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RiskLevelOption(
            icon = "⚖️",
            title = "Balanced",
            description = "Mix of safety and growth with ~65% in stable investments",
            isSelected = selectedRisk == RiskLevel.BALANCED,
            onClick = { onRiskSelected(RiskLevel.BALANCED) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        RiskLevelOption(
            icon = "🚀",
            title = "Aggressive",
            description = "Growth focused with ~50% in higher-risk investments",
            isSelected = selectedRisk == RiskLevel.AGGRESSIVE,
            onClick = { onRiskSelected(RiskLevel.AGGRESSIVE) }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RiskLevelOption(
    icon: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun FinancialSituationStep(
    hasDebts: Boolean,
    onDebtsChange: (Boolean) -> Unit,
    emergencyFund: String,
    onEmergencyFundChange: (String) -> Unit,
    educationStatus: EducationStatus,
    onEducationStatusChange: (EducationStatus) -> Unit,
    employmentStatus: EmploymentStatus,
    onEmploymentStatusChange: (EmploymentStatus) -> Unit,
    livingSituation: LivingSituation,
    onLivingSituationChange: (LivingSituation) -> Unit,
    occupation: String,
    onOccupationChange: (String) -> Unit,
    mainAccountBalance: String,
    onMainAccountBalanceChange: (String) -> Unit,
    savingsAccountBalance: String,
    onSavingsAccountBalanceChange: (String) -> Unit,
    vaultsBalance: String,
    onVaultsBalanceChange: (String) -> Unit,
    subscriptions: List<SubscriptionDraft>,
    onAddSubscription: () -> Unit,
    onSubscriptionNameChange: (Long, String) -> Unit,
    onSubscriptionAmountChange: (Long, String) -> Unit,
    onRemoveSubscription: (Long) -> Unit,
    age: Int,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "💼",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your current situation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Help us recommend the right savings accounts for you",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Education Status (only show if age < 30)
        if (age < 30) {
            Text(
                text = "Education Status",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EducationStatus.entries.forEach { status ->
                    FilterChip(
                        selected = educationStatus == status,
                        onClick = { onEducationStatusChange(status) },
                        label = { Text(status.displayName()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Employment Status
        Text(
            text = "Employment Status",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EmploymentStatus.entries.forEach { status ->
                FilterChip(
                    selected = employmentStatus == status,
                    onClick = { onEmploymentStatusChange(status) },
                    label = { Text(status.displayName()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Living situation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LivingSituation.entries.forEach { option ->
                FilterChip(
                    selected = livingSituation == option,
                    onClick = { onLivingSituationChange(option) },
                    label = { Text(option.displayName()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = occupation,
            onValueChange = onOccupationChange,
            label = { Text("Occupation (optional)") },
            placeholder = { Text("e.g. Product designer") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Work, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Do you have any debts?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = !hasDebts,
                onClick = { onDebtsChange(false) },
                label = { Text("No") },
                modifier = Modifier.weight(1f),
                leadingIcon = if (!hasDebts) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
            FilterChip(
                selected = hasDebts,
                onClick = { onDebtsChange(true) },
                label = { Text("Yes") },
                modifier = Modifier.weight(1f),
                leadingIcon = if (hasDebts) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = emergencyFund,
            onValueChange = onEmergencyFundChange,
            label = { Text("Current Emergency Fund") },
            placeholder = { Text("0") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
            },
            supportingText = {
                Text("How much do you already have saved for emergencies?")
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Account balances",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Share your current cash cushions so we can account for them in your emergency fund target.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mainAccountBalance,
            onValueChange = onMainAccountBalanceChange,
            label = { Text("Main account balance") },
            placeholder = { Text("0") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.AccountBalance, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = savingsAccountBalance,
            onValueChange = onSavingsAccountBalanceChange,
            label = { Text("Savings account balance") },
            placeholder = { Text("0") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Savings, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = vaultsBalance,
            onValueChange = onVaultsBalanceChange,
            label = { Text("Existing vault or sinking funds") },
            placeholder = { Text("0") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Monthly subscriptions (optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "List recurring bills so Sparely can bake them into your runway calculations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (subscriptions.isEmpty()) {
            Text(
                text = "No subscriptions yet. Add streaming services, rent, insurance, anything that hits monthly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            subscriptions.forEachIndexed { index, draft ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Subscription ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { onRemoveSubscription(draft.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove subscription")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { onSubscriptionNameChange(draft.id, it) },
                            label = { Text("Name") },
                            placeholder = { Text("e.g. Rent or Spotify") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = draft.amount,
                            onValueChange = { onSubscriptionAmountChange(draft.id, it) },
                            label = { Text("Monthly amount") },
                            placeholder = { Text("0") },
                            prefix = { Text("$") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        OutlinedButton(onClick = onAddSubscription, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add subscription")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun GoalStep(
    goal: String,
    onGoalChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "🎯",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "What's your main goal?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Let's focus on what matters most to you",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedTextField(
            value = goal,
            onValueChange = onGoalChange,
            label = { Text("Primary Savings Goal (optional)") },
            placeholder = { Text("e.g., Buy a house, Emergency fund, Vacation") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Flag, contentDescription = null)
            },
            supportingText = {
                Text("You can add more detailed goals later")
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val accentColor = MaterialTheme.colorScheme.primary
        val accentContainer = MaterialTheme.colorScheme.primaryContainer
        Card(
            colors = CardDefaults.cardColors(
                containerColor = accentContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "You're all set!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sparely will now help you automatically save from every purchase, track your progress, and achieve your financial goals.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Saving!", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CountrySelectionStep(
    selectedCountry: CountryConfig?,
    onCountrySelected: (CountryConfig) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Choose your country",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "We'll customize the app for your region with local currency, tax rates, and financial recommendations",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Country selection cards
        CountryProfiles.ALL_COUNTRIES.forEach { country ->
            Card(
                onClick = {
                    onCountrySelected(country)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedCountry?.countryCode == country.countryCode) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = country.countryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${country.languageName} • ${CurrencyPresets.getByCode(country.defaultCurrency)?.symbol ?: country.defaultCurrency}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (selectedCountry?.countryCode == country.countryCode) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedCountry != null
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun WelcomeStep(
    countryConfig: CountryConfig?,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to Sparely",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = buildString {
                append("Smart savings made simple")
                countryConfig?.let {
                    append(" for ${it.countryName}")
                }
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Show country-specific welcome message
        countryConfig?.let { config ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Customized for you",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Currency: ${CurrencyPresets.getByCode(config.defaultCurrency)?.name ?: config.defaultCurrency}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Tax insights: ~${(config.taxConfig.incomeTaxRate * 100).toInt()}% income tax",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Savings goal: ${config.savingsNorms.recommendedEmergencyMonths} months emergency fund",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onSkip) {
            Text("Skip setup")
        }
    }
}
