package com.example.sparely.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.sparely.domain.model.ExpenseCategory

/**
 * Shared utility functions for expense category icons and colors.
 * Used across BudgetScreen, HistoryScreen, and other expense-related UI.
 */

fun getCategoryColor(category: ExpenseCategory): Color {
    return when (category) {
        ExpenseCategory.GROCERIES -> Color(0xFF81C784)    // Green
        ExpenseCategory.DINING -> Color(0xFFEF5350)       // Red (Food)
        ExpenseCategory.TRANSPORTATION -> Color(0xFF42A5F5) // Blue
        ExpenseCategory.ENTERTAINMENT -> Color(0xFFEC407A)  // Pink
        ExpenseCategory.UTILITIES -> Color(0xFF7E57C2)      // Purple
        ExpenseCategory.HEALTH -> Color(0xFF26A69A)         // Teal
        ExpenseCategory.EDUCATION -> Color(0xFF5C6BC0)      // Indigo
        ExpenseCategory.SHOPPING -> Color(0xFF8D6E63)       // Brown
        ExpenseCategory.TRAVEL -> Color(0xFF4FC3F7)         // Light Blue
        ExpenseCategory.OTHER -> Color(0xFF9E9E9E)          // Gray
    }
}

fun getCategoryIcon(category: ExpenseCategory): Int {
    return when (category) {
        ExpenseCategory.GROCERIES -> MaterialSymbols.SHOPPING_CART
        ExpenseCategory.DINING -> MaterialSymbols.RESTAURANT
        ExpenseCategory.TRANSPORTATION -> MaterialSymbols.DIRECTIONS_CAR
        ExpenseCategory.ENTERTAINMENT -> MaterialSymbols.CELEBRATION
        ExpenseCategory.UTILITIES -> MaterialSymbols.LIGHTBULB
        ExpenseCategory.HEALTH -> MaterialSymbols.HEALTH_AND_SAFETY
        ExpenseCategory.EDUCATION -> MaterialSymbols.SCHOOL
        ExpenseCategory.SHOPPING -> MaterialSymbols.SHOPPING_BAG
        ExpenseCategory.TRAVEL -> MaterialSymbols.FLIGHT
        ExpenseCategory.OTHER -> MaterialSymbols.INFO
    }
}
