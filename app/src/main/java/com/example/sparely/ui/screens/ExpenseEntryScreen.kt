package com.example.sparely.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.sparely.ui.components.SparelyChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import com.example.sparely.ui.components.SparelyTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.ExpenseCategory
import com.example.sparely.domain.model.ExpenseInput
import com.example.sparely.domain.model.RecommendationResult
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.SparelySettings
import com.example.sparely.domain.model.SavingsPercentages
import com.example.sparely.ui.components.SparelyButton
import com.example.sparely.ui.components.SparelyTonalButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import com.example.sparely.domain.model.Store
import com.example.sparely.ui.components.SearchableStoreSelector
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import com.example.sparely.domain.model.StoreInput
import com.example.sparely.ui.utils.toSafeDatePickerMillis
import com.example.sparely.ui.utils.filterCurrencyInput
import com.example.sparely.ui.utils.toSafeDouble
import com.example.sparely.ui.utils.toSafeDoubleOrZero
import java.time.Instant
import java.time.ZoneOffset
import com.example.sparely.ui.components.SparelyTextButton
import kotlinx.coroutines.launch
import com.example.sparely.domain.model.PaymentMethod
import androidx.compose.ui.res.stringResource
import com.example.sparely.domain.model.displayName
import com.sparely.app.R
// import com.example.sparely.ui.screens.displayName // Not needed if in same package
import com.example.sparely.ui.components.PaymentMethodSelector
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEntryScreen(
    settings: SparelySettings,
    recommendation: RecommendationResult?,
    vaults: List<SmartVault> = emptyList(),
    stores: List<Store> = emptyList(),
    onSave: (ExpenseInput) -> Unit,
    onCancel: () -> Unit,
    onCreateStore: suspend (StoreInput) -> Store? = { null },
    onEditStore: (Store) -> Unit = {},
    onDeleteStore: (Store) -> Unit = {},
    brandfetchClientId: String? = null,
    paymentMethods: List<PaymentMethod> = emptyList(),
    onManagePaymentMethods: () -> Unit = {},
    brandSearchResults: List<com.example.sparely.data.remote.BrandfetchBrand> = emptyList(),
    onBrandSearch: (String) -> Unit = {},
    prefillExpense: com.example.sparely.domain.model.Expense? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize form fields from prefillExpense if provided
    var description by remember { mutableStateOf(prefillExpense?.description ?: "") }
    var amountText by remember { mutableStateOf(prefillExpense?.amount?.let { String.format("%.2f", it) } ?: "") }
    var category by remember { mutableStateOf(prefillExpense?.category ?: ExpenseCategory.OTHER) }
    var includeTax by remember { mutableStateOf(prefillExpense?.includesTax ?: settings.includeTaxByDefault) }
    var deductFromMainAccount by remember { mutableStateOf(false) }
    var deductFromVaultId by remember { mutableStateOf(prefillExpense?.deductedFromVaultId) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) } // Always today for new expense
    var manualMode by remember { mutableStateOf(!settings.autoRecommendationsEnabled) }
    var emergencyPercent by remember { mutableFloatStateOf(settings.defaultPercentages.emergency.toFloat()) }
    var investPercent by remember { mutableFloatStateOf(settings.defaultPercentages.invest.toFloat()) }
    var funPercent by remember { mutableFloatStateOf(settings.defaultPercentages.`fun`.toFloat()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var vaultDropdownExpanded by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableStateOf<Store?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var notes by remember { mutableStateOf(prefillExpense?.notes ?: "") }
    var orderNumber by remember { mutableStateOf("") } // Don't repeat order number
    
    // Line Items State
    val expenseItems = remember { androidx.compose.runtime.mutableStateListOf<com.example.sparely.domain.model.ExpenseItem>() }
    
    // Auto-sum logic: Update total amount when items change, if manual amount hasn't been touched or is consistent
    androidx.compose.runtime.LaunchedEffect(expenseItems.toList()) {
        val itemsTotal = expenseItems.sumOf { it.totalPrice }
        if (itemsTotal > 0) {
            // Only auto-update if the current amount matches the previous item total (indicating it's driven by items)
            // or if amount is 0/empty
            val currentAmount = amountText.toSafeDoubleOrZero()
            // Simplified logic: If items exist, summing them overrides the manual amount?
            // Or maybe just suggest it? Let's just update it for convenience as requested.
             amountText = String.format("%.2f", itemsTotal)
        }
    }
    
    // Set initial payment method if any default exists
    androidx.compose.runtime.LaunchedEffect(paymentMethods) {
        if (selectedPaymentMethod == null) {
            val default = paymentMethods.find { it.isDefault }
            if (default != null) {
                selectedPaymentMethod = default
                deductFromMainAccount = default.defaultDeductFromMainAccount
            }
        }
    }
    
    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toSafeDatePickerMillis()
    )

    val activeVaults = remember(vaults) { vaults.filter { !it.archived } }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.expense_entry_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = if (manualMode) {
                stringResource(R.string.expense_entry_manual_desc)
            } else {
                stringResource(R.string.expense_entry_auto_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SparelyTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.expense_entry_description_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        SparelyTextField(
            value = amountText,
            onValueChange = { amountText = it.filterCurrencyInput() },
            label = { Text(stringResource(R.string.expense_entry_amount_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        CategorySelector(selected = category, onSelect = { category = it })
        
        // Store/Website selector
        SearchableStoreSelector(
            stores = stores,
            selectedStore = selectedStore,
            onStoreSelected = { selectedStore = it },
            onCreateStore = onCreateStore,
            onEditStore = onEditStore,
            onDeleteStore = onDeleteStore,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            brandfetchClientId = brandfetchClientId,
            brandSearchResults = brandSearchResults,
            onBrandSearch = onBrandSearch
        )
        
        PaymentMethodSelector(
            paymentMethods = paymentMethods,
            selectedMethod = selectedPaymentMethod,
            onMethodSelected = { method ->
                selectedPaymentMethod = method
                method?.let { 
                    // Smart default: If credit card, default to NOT deducting from main account (it adds to debt)
                    // Otherwise (Debit/Cash), default to deducting.
                    deductFromMainAccount = !it.isCreditCard
                }
            },
            onManageMethods = onManagePaymentMethods,
            expenseAmount = amountText.toSafeDoubleOrZero()
        )
        
        // Notes field
        SparelyTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.expense_notes_label)) },
            placeholder = { Text(stringResource(R.string.expense_notes_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )
        
        // Order Number field
        SparelyTextField(
            value = orderNumber,
            onValueChange = { orderNumber = it },
            label = { Text(stringResource(R.string.expense_order_number_label)) },
            placeholder = { Text(stringResource(R.string.expense_order_number_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Line Items Section
        com.example.sparely.ui.components.ExpenseItemsList(
            items = expenseItems,
            onItemsChanged = { newItems ->
                expenseItems.clear()
                expenseItems.addAll(newItems)
            }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.expense_entry_date_label))
                Text(selectedDate.format(dateFormatter), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SparelyTonalButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
            ) {
                Text(stringResource(R.string.expense_entry_change_date_button))
            }
        }
        
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    SparelyTextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    SparelyTextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.expense_entry_auto_allocation_title))
                Text(
                    text = stringResource(R.string.expense_entry_auto_allocation_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = !manualMode, onCheckedChange = { manualMode = !it })
        }
        if (!manualMode) {
            recommendation?.let {
                Surface(
                   color = MaterialTheme.colorScheme.surfaceContainerHigh,
                   shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                   modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.expense_entry_applied_suggestion), 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.expense_entry_suggestion_detail, formatPercent(it.recommendedPercentages.emergency), formatPercent(it.recommendedPercentages.invest), formatPercent(it.recommendedPercentages.`fun`)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            PercentSliders(
                emergency = emergencyPercent,
                invest = investPercent,
                funValue = funPercent,
                onEmergencyChange = { emergencyPercent = it },
                onInvestChange = { investPercent = it },
                onFunChange = { funPercent = it }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.expense_entry_include_tax_label), style = MaterialTheme.typography.bodyMedium)
            Checkbox(checked = includeTax, onCheckedChange = { includeTax = it })
        }
        // Deduct from Main Account Toggle with Enhanced Explanation
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recurring_deduct_main_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Switch(
                        checked = deductFromMainAccount,
                        onCheckedChange = { deductFromMainAccount = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }
                
                val helperText = when {
                    deductFromMainAccount && selectedPaymentMethod?.isCreditCard == true -> 
                        stringResource(R.string.recurring_deduct_main_desc_credit_on_warning)
                    deductFromMainAccount -> 
                        stringResource(R.string.recurring_deduct_main_desc_debit)
                    !deductFromMainAccount && selectedPaymentMethod?.isCreditCard == true -> 
                        stringResource(R.string.recurring_deduct_main_desc_credit_off)
                    else -> stringResource(R.string.recurring_deduct_main_desc_debit)
                }
                
                val textColor = if (deductFromMainAccount && selectedPaymentMethod?.isCreditCard == true) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }
        
        // Vault selection dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.expense_entry_deduct_vault_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (activeVaults.isEmpty()) {
                    stringResource(R.string.expense_entry_no_vaults)
                } else if (deductFromVaultId != null && deductFromMainAccount) {
                    stringResource(R.string.expense_entry_vault_overflow)
                } else {
                    stringResource(R.string.expense_entry_choose_vault_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (deductFromVaultId != null && deductFromMainAccount) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (activeVaults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = vaultDropdownExpanded,
                    onExpandedChange = { vaultDropdownExpanded = it }
                ) {
                    SparelyTextField(
                        value = deductFromVaultId?.let { id -> 
                            activeVaults.find { it.id == id }?.name ?: stringResource(R.string.expense_entry_none)
                        } ?: stringResource(R.string.expense_entry_none),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vaultDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        label = { Text(stringResource(R.string.recurring_choose_vault)) }
                    )
                    ExposedDropdownMenu(
                        expanded = vaultDropdownExpanded,
                        onDismissRequest = { vaultDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(stringResource(R.string.expense_entry_none))
                                    Text(
                                        text = stringResource(R.string.expense_entry_none_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                deductFromVaultId = null
                                vaultDropdownExpanded = false
                            }
                        )
                        for (vault in activeVaults) {
                    DropdownMenuItem(
                        text = { Text(vault.name) },
                        onClick = {
                            deductFromVaultId = vault.id
                            vaultDropdownExpanded = false
                        },
                        trailingIcon = {
                            Text(
                                text = "$${String.format("%.2f", vault.currentBalance)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                    }
                }
            }
        }
        
        errorText?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SparelyButton(onClick = {
                val amount = amountText.toSafeDouble()
                if (amount == null || amount <= 0.0) {
                    errorText = context.getString(R.string.expense_entry_error_amount)
                    return@SparelyButton
                }
                
                scope.launch {
                    var finalStoreId = selectedStore?.id
                    
                    // Auto-resolve store if name typed but not selected
                    if (finalStoreId == null && searchQuery.isNotBlank()) {
                         val existing = stores.find { it.name.equals(searchQuery.trim(), ignoreCase = true) }
                         if (existing != null) {
                             finalStoreId = existing.id
                         } else {
                             // Create new store
                             val newStore = onCreateStore(StoreInput(name = searchQuery.trim()))
                             finalStoreId = newStore?.id
                         }
                    }
                    
                    val manualPercentages = if (manualMode) {
                        SavingsPercentages(
                            emergency = emergencyPercent.toDouble(),
                            invest = investPercent.toDouble(),
                            `fun` = funPercent.toDouble(),
                            safeInvestmentSplit = settings.defaultPercentages.safeInvestmentSplit
                        ).adjustWithinBudget()
                    } else {
                        null
                    }
                    errorText = null
                    onSave(
                        ExpenseInput(
                            description = description,
                            amount = amount,
                            category = category,
                            date = selectedDate,
                            includesTax = includeTax,
                            manualPercentages = manualPercentages,
                            deductFromMainAccount = deductFromMainAccount,
                            deductFromVaultId = deductFromVaultId,
                            storeId = finalStoreId,
                            paymentMethodId = selectedPaymentMethod?.id,
                            notes = notes.takeIf { it.isNotBlank() },
                            orderNumber = orderNumber.takeIf { it.isNotBlank() },
                            items = expenseItems.toList()
                        )
                    )
                }
            }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save))
            }
            SparelyTonalButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(
    selected: ExpenseCategory,
    onSelect: (ExpenseCategory) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.expense_entry_category_title), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (category in ExpenseCategory.entries) {
                SparelyChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text(category.displayName()) }
                )
            }
        }
    }
}

@Composable
private fun PercentSliders(
    emergency: Float,
    invest: Float,
    funValue: Float,
    onEmergencyChange: (Float) -> Unit,
    onInvestChange: (Float) -> Unit,
    onFunChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.expense_entry_manual_allocation_title), style = MaterialTheme.typography.titleSmall)
        AllocationSlider(label = stringResource(R.string.onboarding_financial_emergency_title), value = emergency, onValueChange = onEmergencyChange)
        AllocationSlider(label = stringResource(R.string.onboarding_financial_invest_title), value = invest, onValueChange = onInvestChange)
        AllocationSlider(label = stringResource(R.string.onboarding_financial_fun_title), value = funValue, onValueChange = onFunChange)
        val total = emergency + invest + funValue
        Text(stringResource(R.string.expense_entry_total_allocation, formatPercent(total.toDouble())), color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
    }
}

@Composable
private fun AllocationSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(formatPercent(value.toDouble()))
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(it.coerceIn(0f, 0.5f)) },
            valueRange = 0f..0.5f
        )
    }
}

private fun formatPercent(value: Double): String = String.format("%.1f%%", value.coerceIn(0.0, 1.0) * 100)
