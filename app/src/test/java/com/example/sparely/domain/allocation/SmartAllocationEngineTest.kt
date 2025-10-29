package com.example.sparely.domain.allocation

import com.example.sparely.domain.model.SmartVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SmartAllocationEngineTest {

    @Test
    fun allocate_proportional_when_insufficient() {
        val v1 = SmartVault(id = 1L, name = "A", targetAmount = 0.0, currentBalance = 0.0, monthlyNeed = 600.0, priorityWeight = 1.0)
        val v2 = SmartVault(id = 2L, name = "B", targetAmount = 0.0, currentBalance = 0.0, monthlyNeed = 300.0, priorityWeight = 2.0)

        val input = SmartAllocationEngine.AllocationInput(
            vaults = listOf(v1, v2),
            monthlyIncome = 600.0,
            mainAccountBalance = 1000.0,
            safeBufferPercent = 0.0,
            today = LocalDate.now()
        )

        val result = SmartAllocationEngine.allocate(input)

        // available=600, weights 1 and 2 -> allocations 200 and 400
        assertEquals(200.0, result.allocations[1L] ?: 0.0, 0.001)
        assertEquals(400.0, result.allocations[2L] ?: 0.0, 0.001)
    }

    @Test
    fun flow_goal_ramping_applies() {
        val start = LocalDate.now().plusMonths(2)
        val v = SmartVault(id = 3L, name = "Flow", targetAmount = 0.0, currentBalance = 0.0, monthlyNeed = 300.0, startDate = start)

        val input = SmartAllocationEngine.AllocationInput(
            vaults = listOf(v),
            monthlyIncome = 300.0,
            mainAccountBalance = 0.0,
            safeBufferPercent = 0.0,
            today = LocalDate.now(),
            rampWindowMonths = 3
        )

        val result = SmartAllocationEngine.allocate(input)

        // ramp multiplier now yields 70% of monthly need when start is 2 months away -> 210
        assertEquals(210.0, result.allocations[3L] ?: 0.0, 0.001)
    }

    @Test
    fun detects_auto_archive_for_completed_vault() {
        val v = SmartVault(id = 5L, name = "Done", targetAmount = 100.0, currentBalance = 120.0)
        val input = SmartAllocationEngine.AllocationInput(vaults = listOf(v), monthlyIncome = 100.0, mainAccountBalance = 0.0)
        val result = SmartAllocationEngine.allocate(input)
        assertTrue(result.archiveVaultIds.contains(5L))
    }

    @Test
    fun pending_contributions_reduce_new_allocations_for_fixed_goals() {
        val today = LocalDate.now()
        val car = SmartVault(
            id = 10L,
            name = "Car",
            targetAmount = 5000.0,
            currentBalance = 0.0,
            targetDate = today.plusMonths(9),
            priorityWeight = 1.0
        )
        val flow = SmartVault(
            id = 11L,
            name = "Flow",
            targetAmount = 0.0,
            currentBalance = 0.0,
            monthlyNeed = 600.0,
            startDate = today.plusMonths(1),
            priorityWeight = 1.0
        )

        val input = SmartAllocationEngine.AllocationInput(
            vaults = listOf(car, flow),
            monthlyIncome = 1200.0,
            mainAccountBalance = 0.0,
            safeBufferPercent = 0.0,
            maxAllocationPercent = 1.0,
            today = today,
            pendingContributions = mapOf(car.id to 5000.0)
        )

        val result = SmartAllocationEngine.allocate(input)

        assertEquals(0.0, result.allocations[car.id] ?: 0.0, 0.001)
        assertEquals(600.0, result.allocations[flow.id] ?: 0.0, 0.001)
    }

    @Test
    fun compute_vault_deduction_overflow() {
        val (deduct, overflow) = SmartAllocationEngine.computeVaultDeduction(expenseAmount = 150.0, vaultBalance = 100.0)
        assertEquals(100.0, deduct, 0.001)
        assertEquals(50.0, overflow, 0.001)
    }
}
