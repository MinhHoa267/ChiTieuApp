package com.example.chitieuapp.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chitieuapp.ui.components.DailyBarChart
import com.example.chitieuapp.ui.components.DayAxisLabels
import com.example.chitieuapp.ui.components.DonutChart
import com.example.chitieuapp.ui.components.EmptyState
import com.example.chitieuapp.ui.components.MonthSelector
import com.example.chitieuapp.ui.theme.incomeColor
import com.example.chitieuapp.ui.util.formatMoney
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Thống kê") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MonthSelector(state.month, viewModel::previousMonth, viewModel::nextMonth)

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !state.showIncome,
                    onClick = { viewModel.setShowIncome(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Chi tiêu") }
                SegmentedButton(
                    selected = state.showIncome,
                    onClick = { viewModel.setShowIncome(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Thu nhập") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Thu: ${formatMoney(state.totalIncome)}", color = incomeColor())
                Text("Chi: ${formatMoney(state.totalExpense)}", color = MaterialTheme.colorScheme.error)
            }

            if (state.slices.isEmpty()) {
                EmptyState("Chưa có dữ liệu trong tháng này.")
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DonutChart(state.slices)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (state.showIncome) "Tổng thu" else "Tổng chi",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(formatMoney(state.total), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        state.slices.forEach { slice -> SliceRow(slice) }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Theo ngày trong tháng", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))
                        DailyBarChart(
                            values = state.daily,
                            barColor = if (state.showIncome) incomeColor() else MaterialTheme.colorScheme.primary,
                        )
                        DayAxisLabels(daysInMonth = state.daily.size)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SliceRow(slice: CategorySlice) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(Color(slice.color), CircleShape))
        Text(slice.name, Modifier.padding(start = 8.dp).weight(1f))
        Text("${slice.percent.roundToInt()}%", style = MaterialTheme.typography.labelMedium)
        Text(
            formatMoney(slice.amount),
            Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
