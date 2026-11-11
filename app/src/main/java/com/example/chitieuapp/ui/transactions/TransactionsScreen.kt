package com.example.chitieuapp.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chitieuapp.domain.TransactionItem
import com.example.chitieuapp.ui.components.EmptyState
import com.example.chitieuapp.ui.components.MonthSelector
import com.example.chitieuapp.ui.components.TransactionRow
import com.example.chitieuapp.ui.theme.incomeColor
import com.example.chitieuapp.ui.util.formatDateHeader
import com.example.chitieuapp.ui.util.formatMoney
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<TransactionItem?>(null) }
    val groups = remember(state.items) { state.items.groupBy { it.tx.dateEpochDay }.toList() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Giao dịch") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm giao dịch")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                MonthSelector(
                    month = state.month,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("Tất cả") },
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Thu: ${formatMoney(state.income)}", color = incomeColor())
                    Text("Chi: ${formatMoney(state.expense)}", color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider()
            }

            if (groups.isEmpty()) {
                item { EmptyState("Không có giao dịch nào trong tháng này.") }
            }
            groups.forEach { (epochDay, items) ->
                item(key = "header_$epochDay") {
                    Text(
                        formatDateHeader(LocalDate.ofEpochDay(epochDay)),
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(items, key = { it.tx.id }) { item ->
                    TransactionRow(
                        item = item,
                        onClick = { onOpenTransaction(item.tx.id) },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xoá giao dịch?") },
            text = { Text("${item.categoryName}: ${formatMoney(item.tx.amount)}. Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item.tx.id)
                    pendingDelete = null
                }) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Huỷ") } },
        )
    }
}
