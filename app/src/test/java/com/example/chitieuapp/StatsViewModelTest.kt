package com.example.chitieuapp

import com.example.chitieuapp.ui.stats.StatsViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val transactions = listOf(
        tx("a", 300_000, LocalDate.of(2026, 11, 5), categoryId = "food"),
        tx("b", 100_000, LocalDate.of(2026, 11, 5), categoryId = "transport"),
        tx("c", 500_000, LocalDate.of(2026, 10, 20), categoryId = "food"), // tháng khác
        tx("d", 10_000_000, LocalDate.of(2026, 11, 1), isIncome = true, categoryId = "salary"),
    )

    private fun viewModel() = StatsViewModel(
        FakeTransactionRepository(transactions), FakeCategoryRepository(), TEST_CLOCK
    )

    @Test
    fun expenseSlices_areGroupedByCategoryAndSortedDescending() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val state = vm.state.value
        assertEquals(400_000L, state.total)
        assertEquals(listOf("food", "transport"), state.slices.map { it.categoryId })
        assertEquals(75f, state.slices[0].percent, 0.01f)
        assertEquals(25f, state.slices[1].percent, 0.01f)
    }

    @Test
    fun dailyTotals_haveOneEntryPerDayOfMonth() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val daily = vm.state.value.daily
        assertEquals(30, daily.size) // tháng 11 có 30 ngày
        assertEquals(400_000L, daily[4]) // ngày 5
        assertEquals(0L, daily[0])
    }

    @Test
    fun switchingToIncome_showsOnlyIncomeCategories() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.setShowIncome(true)

        val state = vm.state.value
        assertEquals(10_000_000L, state.total)
        assertEquals(listOf("salary"), state.slices.map { it.categoryId })
        assertEquals(100f, state.slices.single().percent, 0.01f)
        assertEquals(10_000_000L, state.daily[0]) // ngày 1
    }

    @Test
    fun previousMonth_changesMonthAndData() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.previousMonth()

        val state = vm.state.value
        assertEquals(10, state.month.monthValue)
        assertEquals(500_000L, state.total)
        assertEquals(31, state.daily.size)
    }
}
