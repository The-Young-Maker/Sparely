package com.example.sparely.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sparely.domain.model.ExpenseItem
import com.example.sparely.ui.utils.filterCurrencyInput
import com.example.sparely.ui.utils.toSafeDoubleOrZero
import com.sparely.app.R

@Composable
fun ExpenseItemsList(
    items: List<ExpenseItem>,
    onItemsChanged: (List<ExpenseItem>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Items / Products",
            style = MaterialTheme.typography.titleSmall
        )

        items.forEachIndexed { index, item ->
            ExpenseItemRow(
                item = item,
                onUpdate = { updated ->
                    val newlist = items.toMutableList()
                    newlist[index] = updated
                    onItemsChanged(newlist)
                },
                onRemove = {
                    val newlist = items.toMutableList()
                    newlist.removeAt(index)
                    onItemsChanged(newlist)
                }
            )
        }

        SparelyTonalButton(
            onClick = {
                onItemsChanged(items + ExpenseItem(expenseId = 0, name = "", unitPrice = 0.0, quantity = 1))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Item")
        }
    }
}

@Composable
private fun ExpenseItemRow(
    item: ExpenseItem,
    onUpdate: (ExpenseItem) -> Unit,
    onRemove: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SparelyTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Product Name") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SparelyTextField(
                    value = if (item.quantity > 0) item.quantity.toString() else "",
                    onValueChange = { 
                        val qty = it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0
                        onUpdate(item.copy(quantity = qty, totalPrice = qty * item.unitPrice))
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(0.3f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                SparelyTextField(
                    value = if (item.unitPrice > 0.0) item.unitPrice.toString() else "",
                    onValueChange = {
                        val price = it.filterCurrencyInput().toSafeDoubleOrZero()
                        onUpdate(item.copy(unitPrice = price, totalPrice = item.quantity * price))
                    },
                    label = { Text("Price") },
                    modifier = Modifier.weight(0.7f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$") }
                )
            }
        }
    }
}
