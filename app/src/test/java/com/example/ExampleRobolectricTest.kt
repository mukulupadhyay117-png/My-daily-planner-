package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RepeatRule
import com.example.data.repository.PlannerRepository
import com.example.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My Daily Planner", appName)
    }

    @Test
    fun `streak calculation counts consecutive days accurately`() {
        val today = "2026-11-19"
        val dates = setOf("2026-11-19", "2026-11-18", "2026-11-17", "2026-11-14")
        assertEquals(3, PlannerRepository.calculateCurrentStreak(dates, today))
        assertEquals(3, PlannerRepository.calculateBestStreak(dates))
    }

    @Test
    fun `repeating task advances next occurrence correctly`() {
        val base = "2026-11-19"
        assertEquals("2026-11-20", DateTimeUtils.nextOccurrenceDateKey(base, RepeatRule.DAILY))
        assertEquals("2026-11-26", DateTimeUtils.nextOccurrenceDateKey(base, RepeatRule.WEEKLY))
        assertEquals("2026-12-19", DateTimeUtils.nextOccurrenceDateKey(base, RepeatRule.MONTHLY))
    }

    @Test
    fun `month grid always returns 42 calendar cells`() {
        val cells = DateTimeUtils.buildMonthGrid(2026, 10) // November 2026
        assertEquals(42, cells.size)
        assertTrue(cells.any { it.dateKey == "2026-11-19" && it.isCurrentMonth })
    }
}
