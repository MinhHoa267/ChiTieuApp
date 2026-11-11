package com.example.chitieuapp.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.ui.components.CategoryBadge
import com.example.chitieuapp.ui.components.MonthSelector
import com.example.chitieuapp.ui.util.formatMoney
import kotlin.math.roundToInt

private val categoryPalette = listOf(
    0xFFEF5350, 0xFFFFA726, 0xFFFFEE58, 0xFF66BB6A,
    0xFF26A69A, 0xFF42A5F5, 0xFF5C6BC0, 0xFFAB47BC,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showIncomeCategories by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Ngân sách & danh mục") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthSelector(state.month, viewModel::previousMonth, viewModel::nextMonth)

            BudgetCard(
                state = state,
                onLimitChange = viewModel::onLimitChange,
                onSave = viewModel::saveBudget,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Danh mục", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Thêm", modifier = Modifier.padding(start = 4.dp))
                }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !showIncomeCategories,
                    onClick = { showIncomeCategories = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Chi tiêu") }
                SegmentedButton(
                    selected = showIncomeCategories,
                    onClick = { showIncomeCategories = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Thu nhập") }
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    state.categories.filter { it.isIncome == showIncomeCategories }.forEach { category ->
                        ListItem(
                            leadingContent = { CategoryBadge(category.name, category.colorArgb) },
                            headlineContent = { Text(category.name) },
                            supportingContent = if (category.isDefault) ({ Text("Mặc định") }) else null,
                            trailingContent = if (!category.isDefault) ({
                                IconButton(onClick = { pendingDelete = category }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Xoá danh mục")
                                }
                            }) else null,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            initialIsIncome = showIncomeCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, isIncome, color ->
                viewModel.addCategory(name, isIncome, color)
                showAddDialog = false
            },
        )
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xoá danh mục \"${category.name}\"?") },
            text = { Text("Các giao dịch cũ thuộc danh mục này sẽ hiển thị là \"Khác\".") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category.id)
                    pendingDelete = null
                }) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun BudgetCard(
    state: BudgetUiState,
    onLimitChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hạn mức chi tiêu tháng ${state.month.monthValue}", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.limitText,
                onValueChange = onLimitChange,
                label = { Text("Ngân sách (₫)") },
                isError = state.error != null,
                supportingText = { state.error?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Lưu ngân sách") }

            if (state.limit != null) {
                val barColor = when {
                    state.progress >= 1f -> MaterialTheme.colorScheme.error
                    state.progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                LinearProgressIndicator(
                    progress = { state.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = barColor,
                )
                Text(
                    "Đã chi ${formatMoney(state.spent)} / ${formatMoney(state.limit)} " +
                        "(${(state.progress * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.progress >= 1f) {
                    Text(
                        "Bạn đã vượt ngân sách ${formatMoney(state.spent - state.limit)}.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    initialIsIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, isIncome: Boolean, colorArgb: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(initialIsIncome) }
    var color by remember { mutableStateOf(categoryPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm danh mục") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Tên danh mục") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Chi tiêu") })
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Thu nhập") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryPalette.forEach { c ->
                        Box(
                            Modifier
                                .size(if (c == color) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, isIncome, color) }, enabled = name.isNotBlank()) {
                Text("Thêm")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}
