package com.example.sparely

import org.junit.Test

import org.junit.Assert.*
import java.time.LocalDate
import com.example.sparely.domain.model.SmartVault
import com.example.sparely.domain.model.calculateMonthlyContribution
import com.example.sparely.domain.model.monthsUntil

class VaultHelpersTest {
    @Test
    fun monthsUntil_calculation() {
        val today = LocalDate.of(2025, 10, 1)
        val target = LocalDate.of(2026, 1, 15)
        val vault = SmartVault(id = 1, name = "Test", targetAmount = 1000.0, currentBalance = 0.0, targetDate = target)
        val months = vault.monthsUntil(target, today)
        assertEquals(3, months)
    }

    @Test
    fun calculateMonthlyContribution_targetDate() {
        val today = LocalDate.of(2025, 10, 1)
        val target = LocalDate.of(2026, 1, 1)
        val vault = SmartVault(id = 1, name = "Test", targetAmount = 1200.0, currentBalance = 200.0, targetDate = target)
        val monthly = vault.calculateMonthlyContribution(today)
        // Remaining 1000 over 3 months -> ~333.33
        assertTrue(monthly > 333.0 && monthly < 334.0)
    }
}

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}