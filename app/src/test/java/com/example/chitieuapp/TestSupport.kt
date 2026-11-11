package com.example.chitieuapp

import android.net.Uri
import com.example.chitieuapp.data.local.BudgetEntity
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.local.TransactionEntity
import com.example.chitieuapp.data.receipt.ReceiptStore
import com.example.chitieuapp.data.repository.BudgetRepository
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Thời điểm cố định cho test: 09/11/2026. */
val TEST_CLOCK: Clock = Clock.fixed(Instant.parse("2026-11-09T05:00:00Z"), ZoneOffset.UTC)

val FOOD = CategoryEntity("food", "Ăn uống", false, 0xFFEF5350L, isDefault = true)
val TRANSPORT = CategoryEntity("transport", "Di chuyển", false, 0xFF42A5F5L, isDefault = true)
val SALARY = CategoryEntity("salary", "Lương", true, 0xFF66BB6AL, isDefault = true)
val TEST_CATEGORIES = listOf(FOOD, TRANSPORT, SALARY)

fun tx(
    id: String,
    amount: Long,
    date: LocalDate,
    isIncome: Boolean = false,
    categoryId: String = "food",
) = TransactionEntity(
    id = id,
    amount = amount,
    isIncome = isIncome,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDay(),
    createdAt = 0L,
    updatedAt = 0L,
)

/** Thay Dispatchers.Main bằng dispatcher test (viewModelScope dùng Main). */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeTransactionRepository(initial: List<TransactionEntity> = emptyList()) : TransactionRepository {
    val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<TransactionEntity>> = items
    override suspend fun getById(id: String) = items.value.find { it.id == id }
    override suspend fun save(transaction: TransactionEntity) =
        items.update { list -> list.filter { it.id != transaction.id } + transaction }
    override suspend fun delete(id: String) = items.update { list -> list.filter { it.id != id } }
}

class FakeCategoryRepository(initial: List<CategoryEntity> = TEST_CATEGORIES) : CategoryRepository {
    val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<CategoryEntity>> = items
    override suspend fun ensureDefaults() = Unit
    override suspend fun save(category: CategoryEntity) =
        items.update { list -> list.filter { it.id != category.id } + category }
    override suspend fun delete(id: String) = items.update { list -> list.filter { it.id != id } }
}

class FakeBudgetRepository(initial: Map<String, Long> = emptyMap()) : BudgetRepository {
    val budgets = MutableStateFlow(initial)

    override fun observe(month: String): Flow<BudgetEntity?> =
        budgets.map { map -> map[month]?.let { BudgetEntity(month, it, updatedAt = 0L) } }

    override suspend fun setBudget(month: String, limit: Long) =
        budgets.update { it + (month to limit) }
}

class FakeReceiptStore : ReceiptStore {
    override fun newCaptureUri(): Uri = error("Không dùng trong unit test")
    override suspend fun import(uri: Uri): String? = null
}
