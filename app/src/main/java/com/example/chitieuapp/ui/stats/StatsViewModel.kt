package com.example.chitieuapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.TransactionRepository
import com.example.chitieuapp.domain.UNKNOWN_CATEGORY_COLOR
import com.example.chitieuapp.domain.UNKNOWN_CATEGORY_NAME
import com.example.chitieuapp.domain.inMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CategorySlice(
    val categoryId: String,
    val name: String,
    val color: Long,
    val amount: Long,
    val percent: Float,
)

data class StatsUiState(
    val month: YearMonth,
    val showIncome: Boolean = false,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    /** Phân bổ theo danh mục của loại đang chọn (chi hoặc thu), giảm dần theo số tiền. */
    val slices: List<CategorySlice> = emptyList(),
    /** Tổng theo từng ngày trong tháng (index 0 = ngày 1) của loại đang chọn. */
    val daily: List<Long> = emptyList(),
) {
    val total: Long get() = if (showIncome) totalIncome else totalExpense
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    transactions: TransactionRepository,
    categories: CategoryRepository,
    clock: Clock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val showIncome = MutableStateFlow(false)

    val state: StateFlow<StatsUiState> = combine(
        transactions.observeAll(), categories.observeAll(), month, showIncome
    ) { txs, cats, m, income ->
        val inMonth = txs.filter { it.inMonth(m) }
        val selected = inMonth.filter { it.isIncome == income }
        val total = selected.sumOf { it.amount }
        val byId = cats.associateBy { it.id }

        val slices = selected.groupBy { it.categoryId }
            .map { (id, list) ->
                val amount = list.sumOf { it.amount }
                CategorySlice(
                    categoryId = id,
                    name = byId[id]?.name ?: UNKNOWN_CATEGORY_NAME,
                    color = byId[id]?.colorArgb ?: UNKNOWN_CATEGORY_COLOR,
                    amount = amount,
                    percent = if (total > 0) amount * 100f / total else 0f,
                )
            }
            .sortedByDescending { it.amount }

        val daily = LongArray(m.lengthOfMonth())
        selected.forEach { daily[LocalDate.ofEpochDay(it.dateEpochDay).dayOfMonth - 1] += it.amount }

        StatsUiState(
            month = m,
            showIncome = income,
            totalIncome = inMonth.filter { it.isIncome }.sumOf { it.amount },
            totalExpense = inMonth.filter { !it.isIncome }.sumOf { it.amount },
            slices = slices,
            daily = daily.toList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState(month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
    fun setShowIncome(value: Boolean) { showIncome.value = value }
}
