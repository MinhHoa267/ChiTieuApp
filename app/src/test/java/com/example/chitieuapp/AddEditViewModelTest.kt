package com.example.chitieuapp

import androidx.lifecycle.SavedStateHandle
import com.example.chitieuapp.ui.addedit.AddEditViewModel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTransactionRepository = FakeTransactionRepository(),
        id: String? = null,
    ) = AddEditViewModel(
        savedStateHandle = SavedStateHandle(if (id != null) mapOf("id" to id) else emptyMap()),
        transactions = repo,
        categories = FakeCategoryRepository(),
        receipts = FakeReceiptStore(),
        clock = TEST_CLOCK,
    )

    @Test
    fun save_withEmptyAmount_showsErrorAndDoesNotSave() = runTest {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onCategoryChange("food")
        vm.save()

        assertEquals("Nhập số tiền hợp lệ", vm.state.value.amountError)
        assertNull(vm.state.value.categoryError)
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun save_withoutCategory_showsCategoryError() = runTest {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAmountChange("50000")
        vm.save()

        assertEquals("Chọn danh mục", vm.state.value.categoryError)
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun save_withValidInput_persistsTransactionAndEmitsSaved() = runTest {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onAmountChange("50.000đ") // chỉ giữ lại chữ số
        vm.onCategoryChange("food")
        vm.onNoteChange("  Phở bò  ")
        vm.save()

        val saved = repo.items.value.single()
        assertEquals(50_000L, saved.amount)
        assertFalse(saved.isIncome)
        assertEquals("food", saved.categoryId)
        assertEquals("Phở bò", saved.note)
        assertEquals(LocalDate.of(2026, 11, 9).toEpochDay(), saved.dateEpochDay)
        assertEquals(Unit, vm.saved.first())
    }

    @Test
    fun changingType_clearsCategoryAndFiltersCategoryList() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onCategoryChange("food")
        assertEquals(listOf("food", "transport"), vm.state.value.categories.map { it.id })

        vm.onTypeChange(true)

        assertNull(vm.state.value.categoryId)
        assertEquals(listOf("salary"), vm.state.value.categories.map { it.id })
    }

    @Test
    fun editMode_loadsExistingTransaction() = runTest {
        val existing = tx("t1", 120_000, LocalDate.of(2026, 11, 3), categoryId = "transport")
            .copy(note = "Grab")
        val vm = viewModel(FakeTransactionRepository(listOf(existing)), id = "t1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val state = vm.state.value
        assertTrue(state.isEdit)
        assertFalse(state.loading)
        assertEquals("120000", state.amountText)
        assertEquals("transport", state.categoryId)
        assertEquals("Grab", state.note)
        assertEquals(LocalDate.of(2026, 11, 3), state.date)
        assertNotNull(state.categories.find { it.id == "transport" })
    }
}
