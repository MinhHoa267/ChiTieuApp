package com.example.chitieuapp

import com.example.chitieuapp.ui.dashboard.DashboardViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val transactions = listOf(
        tx("salary", 20_000_000, LocalDate.of(2026, 11, 1), isIncome = true, categoryId = "salary"),
        tx("lunch", 150_000, LocalDate.of(2026, 11, 5)),
        tx("old", 50_000, LocalDate.of(2026, 10, 30)), // tháng trước: không được tính
    )

    @Test
    fun totals_onlyCountCurrentMonth() = runTest {
        val vm = DashboardViewModel(
            FakeTransactionRepository(transactions),
            FakeCategoryRepository(),
            FakeBudgetRepository(),
            TEST_CLOCK,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(20_000_000L, state.income)
        assertEquals(150_000L, state.expense)
        assertEquals(19_850_000L, state.balance)
    }

    @Test
    fun budgetProgress_isExpenseOverLimit() = runTest {
        val vm = DashboardViewModel(
            FakeTransactionRepository(transactions),
            FakeCategoryRepository(),
            FakeBudgetRepository(mapOf("2026-11" to 1_000_000L)),
            TEST_CLOCK,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertEquals(1_000_000L, vm.state.value.budgetLimit)
        assertNotNull(vm.state.value.budgetProgress)
        assertEquals(0.15f, vm.state.value.budgetProgress!!, 0.0001f)
    }

    @Test
    fun noBudget_progressIsNull() = runTest {
        val vm = DashboardViewModel(
            FakeTransactionRepository(transactions),
            FakeCategoryRepository(),
            FakeBudgetRepository(), // chưa đặt ngân sách
            TEST_CLOCK,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        assertNull(vm.state.value.budgetProgress)
    }

    @Test
    fun recent_isSortedNewestFirst_withCategoryNames() = runTest {
        val vm = DashboardViewModel(
            FakeTransactionRepository(transactions),
            FakeCategoryRepository(),
            FakeBudgetRepository(),
            TEST_CLOCK,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val recent = vm.state.value.recent
        assertEquals(listOf("lunch", "salary", "old"), recent.map { it.tx.id })
        assertEquals("Ăn uống", recent.first().categoryName)
    }
}
