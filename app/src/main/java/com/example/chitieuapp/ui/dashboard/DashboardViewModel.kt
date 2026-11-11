package com.example.chitieuapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.repository.BudgetRepository
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.TransactionRepository
import com.example.chitieuapp.domain.TransactionItem
import com.example.chitieuapp.domain.inMonth
import com.example.chitieuapp.domain.key
import com.example.chitieuapp.domain.sortedForDisplay
import com.example.chitieuapp.domain.toItems
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val month: YearMonth,
    val income: Long = 0,
    val expense: Long = 0,
    val budgetLimit: Long? = null,
    val recent: List<TransactionItem> = emptyList(),
    val loading: Boolean = true,
) {
    val balance: Long get() = income - expense

    /** Tỉ lệ đã chi / ngân sách (có thể > 1). null nếu chưa đặt ngân sách. */
    val budgetProgress: Float?
        get() = budgetLimit?.takeIf { it > 0 }?.let { expense.toFloat() / it }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactions: TransactionRepository,
    categories: CategoryRepository,
    budgets: BudgetRepository,
    clock: Clock,
) : ViewModel() {

    private val month = YearMonth.now(clock)

    val state: StateFlow<DashboardUiState> = combine(
        transactions.observeAll(),
        categories.observeAll(),
        budgets.observe(month.key()),
    ) { txs, cats, budget ->
        val inMonth = txs.filter { it.inMonth(month) }
        DashboardUiState(
            month = month,
            income = inMonth.filter { it.isIncome }.sumOf { it.amount },
            expense = inMonth.filter { !it.isIncome }.sumOf { it.amount },
            budgetLimit = budget?.limitAmount,
            recent = txs.sortedForDisplay().take(5).toItems(cats),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState(month))
}
