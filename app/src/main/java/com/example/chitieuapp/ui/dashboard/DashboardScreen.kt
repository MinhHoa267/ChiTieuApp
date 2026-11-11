package com.example.chitieuapp.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chitieuapp.data.repository.AuthUser
import com.example.chitieuapp.ui.components.EmptyState
import com.example.chitieuapp.ui.components.TransactionRow
import com.example.chitieuapp.ui.theme.incomeColor
import com.example.chitieuapp.ui.util.findActivity
import com.example.chitieuapp.ui.util.formatMoney
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: AuthUser?,
    onSignOut: () -> Unit,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSeeAll: () -> Unit,
    onOpenBudget: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tổng quan") },
                actions = { AccountMenu(user, onSignOut) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm giao dịch")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(state) }
            item { BudgetCard(state, onOpenBudget) }
            item { NotificationPermissionCard() }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Giao dịch gần đây", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onSeeAll) { Text("Xem tất cả") }
                }
            }
            if (state.recent.isEmpty() && !state.loading) {
                item { EmptyState("Chưa có giao dịch. Bấm nút + để thêm giao dịch đầu tiên.") }
            } else {
                items(state.recent, key = { it.tx.id }) { item ->
                    TransactionRow(item = item, onClick = { onOpenTransaction(item.tx.id) })
                }
            }
            item { Spacer(Modifier.height(72.dp)) } // chừa chỗ cho FAB
        }
    }
}

@Composable
private fun AccountMenu(user: AuthUser?, onSignOut: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val initial = (user?.name ?: user?.email)?.firstOrNull()?.uppercase() ?: "?"
    Box(Modifier.padding(end = 8.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = MaterialTheme.colorScheme.onPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            user?.name?.let { Text(it, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall) }
            user?.email?.let {
                Text(
                    it,
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenuItem(
                text = { Text("Đăng xuất") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSignOut()
                },
            )
        }
    }
}

@Composable
private fun SummaryCard(state: DashboardUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text("Số dư tháng ${state.month.monthValue}/${state.month.year}", style = MaterialTheme.typography.labelLarge)
            Text(formatMoney(state.balance), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Thu nhập", style = MaterialTheme.typography.labelMedium)
                    Text(formatMoney(state.income), color = incomeColor(), style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Chi tiêu", style = MaterialTheme.typography.labelMedium)
                    Text(formatMoney(state.expense), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(state: DashboardUiState, onOpenBudget: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Ngân sách tháng", style = MaterialTheme.typography.titleMedium)
            val progress = state.budgetProgress
            if (progress == null) {
                Text(
                    "Bạn chưa đặt ngân sách cho tháng này.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenBudget) { Text("Đặt ngân sách") }
            } else {
                Spacer(Modifier.height(8.dp))
                val barColor = when {
                    progress >= 1f -> MaterialTheme.colorScheme.error
                    progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = barColor,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Đã chi ${formatMoney(state.expense)} / ${formatMoney(state.budgetLimit ?: 0)} " +
                        "(${(progress * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Xin quyền POST_NOTIFICATIONS (Android 13+) đúng cách:
 * giải thích lý do -> hệ thống hỏi -> nếu bị từ chối vĩnh viễn thì dẫn tới cài đặt thông báo của app.
 */
@Composable
private fun NotificationPermissionCard() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        if (!isGranted) {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                )
        }
    }

    if (granted) return

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Bật thông báo", style = MaterialTheme.typography.titleSmall)
            Text(
                if (permanentlyDenied) {
                    "Thông báo đang bị tắt. Hãy bật lại trong cài đặt để nhận cảnh báo khi chi tiêu đạt 80% hoặc vượt ngân sách."
                } else {
                    "Cho phép thông báo để app cảnh báo khi chi tiêu đạt 80% hoặc vượt ngân sách tháng."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    if (permanentlyDenied) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    } else {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            ) { Text(if (permanentlyDenied) "Mở cài đặt" else "Cho phép") }
        }
    }
}
