package com.example.sparely.domain.logic

import com.example.sparely.domain.model.PayInterval
import com.example.sparely.domain.model.PayScheduleSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class PayScheduleCalculatorTest {

    @Test
    fun `next_pay_date_advances_correctly_from_end_of_short_month`() {
        // Scenario: Schedule is 15th and 30th.
        // Last pay was Feb 28th (which was the "30th" pay).
        // Next pay should be March 15th.
        
        val schedule = PayScheduleSettings(
            interval = PayInterval.SEMI_MONTHLY,
            semiMonthlyDay1 = 15,
            semiMonthlyDay2 = 30
        )
        
        // Non-leap year: 2023
        val feb28 = LocalDate.of(2023, Month.FEBRUARY, 28)
        
        // Verify the bug: currently likely returns Feb 28
        val next = PayScheduleCalculator.computeNextPayDate(schedule, feb28)
        
        // Expectation: March 15th
        val expected = LocalDate.of(2023, Month.MARCH, 15)
        
        assertEquals("Next pay date should advance to next month", expected, next)
    }

    @Test
    fun `resolve_upcoming_handles_caught_up_dates`() {
        val schedule = PayScheduleSettings(
            interval = PayInterval.SEMI_MONTHLY,
            semiMonthlyDay1 = 15,
            semiMonthlyDay2 = 30,
            lastPayDate = LocalDate.of(2023, Month.FEBRUARY, 28)
        )
        
        // If today is Feb 28th, and last pay was Feb 28th.
        // Upcoming should be March 15th.
        val today = LocalDate.of(2023, Month.FEBRUARY, 28)
        val upcoming = PayScheduleCalculator.resolveUpcomingPayDate(schedule, today)
        
        assertEquals(LocalDate.of(2023, Month.MARCH, 15), upcoming)
    }
}
