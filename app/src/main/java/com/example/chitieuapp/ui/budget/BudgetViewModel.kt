package com.example.chitieuapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.repository.BudgetRepository
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.TransactionRepository
import com.example.chitieuapp.domain.inMonth
import com.example.chitieuapp.domain.key
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetUiState(
    val month: YearMonth,
    val limit: Long? = null,
    val limitText: String = "",
    val spent: Long = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val error: String? = null,
) {
    /** Tỉ lệ đã chi / ngân sách (có thể > 1). 0 nếu chưa đặt. */
    val progress: Float get() = limit?.takeIf { it > 0 }?.let { spent.toFloat() / it } ?: 0f
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val budgets: BudgetRepository,
    clock: Clock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))

    /** null = người dùng chưa sửa, hiển thị hạn mức hiện có. */
    private val limitInput = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<BudgetUiState> = month.flatMapLatest { m ->
        combine(
            limitInput, error, transactions.observeAll(), categories.observeAll(), budgets.observe(m.key())
        ) { input, err, txs, cats, budget ->
            BudgetUiState(
                month = m,
                limit = budget?.limitAmount,
                limitText = input ?: budget?.limitAmount?.toString().orEmpty(),
                spent = txs.filter { !it.isIncome && it.inMonth(m) }.sumOf { it.amount },
                categories = cats,
                error = err,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState(month.value))

    fun previousMonth() = changeMonth(-1)
    fun nextMonth() = changeMonth(1)

    private fun changeMonth(delta: Long) {
        month.value = month.value.plusMonths(delta)
        limitInput.value = null
        error.value = null
    }

    fun onLimitChange(text: String) {
        limitInput.value = text.filter(Char::isDigit).take(12)
        error.value = null
    }

    fun saveBudget() {
        val limit = (limitInput.value ?: state.value.limitText).toLongOrNull()
        if (limit == null || limit <= 0) {
            error.value = "Nhập hạn mức hợp lệ"
            return
        }
        viewModelScope.launch {
            budgets.setBudget(month.value.key(), limit)
            limitInput.value = null
        }
    }

    fun addCategory(name: String, isIncome: Boolean, colorArgb: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            categories.save(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    name = trimmed.take(30),
                    isIncome = isIncome,
                    colorArgb = colorArgb,
                    isDefault = false,
                    synced = false,
                )
            )
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch { categories.delete(id) }
    }
}
