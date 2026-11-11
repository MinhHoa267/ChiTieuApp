package com.example.chitieuapp.ui.addedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.chitieuapp.ui.util.formatDate
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    onDone: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.saved.collect { onDone() } }

    // Chụp ảnh bằng app camera hệ thống qua FileProvider: không cần quyền CAMERA.
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCaptureResult(success)
    }
    // Chọn ảnh từ thư viện bằng Photo Picker: không cần quyền đọc bộ nhớ.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onImagePicked)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Sửa giao dịch" else "Thêm giao dịch") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state.isEdit) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Xoá giao dịch")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AddEditContent(
            state = state,
            onTypeChange = viewModel::onTypeChange,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onNoteChange = viewModel::onNoteChange,
            onDateChange = viewModel::onDateChange,
            onTakePhoto = {
                try {
                    takePicture.launch(viewModel.createCaptureUri())
                } catch (e: Exception) {
                    scope.launch { snackbar.showSnackbar("Không mở được ứng dụng camera") }
                }
            },
            onPickPhoto = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = viewModel::onRemoveReceipt,
            onSave = viewModel::save,
            modifier = Modifier.padding(padding),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá giao dịch?") },
            text = { Text("Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } },
        )
    }
}

/** Phần giao diện không phụ thuộc ViewModel để dễ viết Compose UI test. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditContent(
    state: AddEditUiState,
    onTypeChange: (Boolean) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.isIncome,
                onClick = { onTypeChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("typeExpense"),
            ) { Text("Chi tiêu") }
            SegmentedButton(
                selected = state.isIncome,
                onClick = { onTypeChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("typeIncome"),
            ) { Text("Thu nhập") }
        }

        OutlinedTextField(
            value = state.amountText,
            onValueChange = onAmountChange,
            label = { Text("Số tiền (₫)") },
            isError = state.amountError != null,
            supportingText = { state.amountError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("amountField"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Danh mục", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = category.id == state.categoryId,
                        onClick = { onCategoryChange(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(Color(category.colorArgb), CircleShape)
                            )
                        },
                    )
                }
            }
            state.categoryError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        DateField(date = state.date, onDateChange = onDateChange)

        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text("Ghi chú") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        ReceiptSection(
            receiptPath = state.receiptPath,
            receiptUrl = state.receiptUrl,
            onTakePhoto = onTakePhoto,
            onPickPhoto = onPickPhoto,
            onRemovePhoto = onRemovePhoto,
        )

        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().testTag("saveButton"),
        ) { Text(if (state.isEdit) "Cập nhật" else "Lưu") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
        Text("Ngày: ${formatDate(date)}", modifier = Modifier.padding(start = 8.dp))
    }

    if (showPicker) {
        // DatePicker làm việc với epoch millis UTC của 00:00 ngày được chọn.
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Huỷ") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun ReceiptSection(
    receiptPath: String?,
    receiptUrl: String?,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ảnh hoá đơn", style = MaterialTheme.typography.titleSmall)

        val model: Any? = receiptPath?.let { File(it) } ?: receiptUrl
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "Ảnh hoá đơn",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onTakePhoto) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Text("Chụp ảnh", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onPickPhoto) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Text("Thư viện", modifier = Modifier.padding(start = 8.dp))
            }
            if (model != null) {
                TextButton(onClick = onRemovePhoto) { Text("Bỏ ảnh") }
            }
        }
    }
}
