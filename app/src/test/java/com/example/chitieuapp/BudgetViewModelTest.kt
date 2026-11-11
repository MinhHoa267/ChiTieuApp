package com.example.chitieuapp

import com.example.chitieuapp.ui.budget.BudgetViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val transactions = listOf(
        tx("a", 300_000, LocalDate.of(2026, 11, 5)),
        tx("b", 200_000, LocalDate.of(2026, 11, 6)),
        tx("c", 9_999_999, LocalDate.of(2026, 11, 7), isIncome = true, categoryId = "salary"), // thu: không tính
    )

    @Test
    fun saveBudget_withInvalidLimit_setsErrorAndDoesNotSave() = runTest {
        val budgets = FakeBudgetRepository()
        val vm = BudgetViewModel(FakeTransactionRepository(transactions), FakeCategoryRepository(), budgets, TEST_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onLimitChange("")
        vm.saveBudget()

        assertEquals("Nhập hạn mức hợp lệ", vm.state.value.error)
        assertTrue(budgets.budgets.value.isEmpty())
    }

    @Test
    fun saveBudget_withValidLimit_storesBudgetForCurrentMonth() = runTest {
        val budgets = FakeBudgetRepository()
        val vm = BudgetViewModel(FakeTransactionRepository(transactions), FakeCategoryRepository(), budgets, TEST_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onLimitChange("2.000.000")
        vm.saveBudget()

        assertEquals(mapOf("2026-11" to 2_000_000L), budgets.budgets.value)
        assertNull(vm.state.value.error)
        assertEquals("2000000", vm.state.value.limitText)
    }

    @Test
    fun spentAndProgress_onlyCountExpensesOfSelectedMonth() = runTest {
        val vm = BudgetViewModel(
            FakeTransactionRepository(transactions),
            FakeCategoryRepository(),
            FakeBudgetRepository(mapOf("2026-11" to 1_000_000L)),
            TEST_CLOCK,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val state = vm.state.value
        assertEquals(500_000L, state.spent)
        assertEquals(0.5f, state.progress, 0.0001f)

        vm.nextMonth() // tháng 12 chưa có ngân sách và chưa có chi tiêu
        assertNull(vm.state.value.limit)
        assertEquals(0L, vm.state.value.spent)
    }

    @Test
    fun addAndDeleteCategory() = runTest {
        val categories = FakeCategoryRepository()
        val vm = BudgetViewModel(FakeTransactionRepository(), categories, FakeBudgetRepository(), TEST_CLOCK)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.addCategory("  Thú cưng  ", isIncome = false, colorArgb = 0xFF66BB6AL)
        val added = categories.items.value.single { !it.isDefault }
        assertEquals("Thú cưng", added.name)

        vm.deleteCategory(added.id)
        assertTrue(categories.items.value.none { it.id == added.id })
    }
}
