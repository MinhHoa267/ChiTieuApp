package com.example.chitieuapp.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.TransactionRepository
import com.example.chitieuapp.domain.TransactionItem
import com.example.chitieuapp.domain.inMonth
import com.example.chitieuapp.domain.sortedForDisplay
import com.example.chitieuapp.domain.toItems
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val month: YearMonth,
    val items: List<TransactionItem> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val income: Long = 0,
    val expense: Long = 0,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    clock: Clock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(clock))
    private val categoryFilter = MutableStateFlow<String?>(null)

    val state: StateFlow<TransactionsUiState> = combine(
        transactions.observeAll(), categories.observeAll(), month, categoryFilter
    ) { txs, cats, m, filter ->
        val inMonth = txs.filter { it.inMonth(m) }
        val shown = inMonth.filter { filter == null || it.categoryId == filter }
        TransactionsUiState(
            month = m,
            items = shown.sortedForDisplay().toItems(cats),
            categories = cats,
            selectedCategoryId = filter,
            income = shown.filter { it.isIncome }.sumOf { it.amount },
            expense = shown.filter { !it.isIncome }.sumOf { it.amount },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState(month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
    fun selectCategory(id: String?) { categoryFilter.value = id }

    fun delete(id: String) {
        viewModelScope.launch { transactions.delete(id) }
    }
}
