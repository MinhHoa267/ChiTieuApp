package com.example.chitieuapp.ui.addedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.local.TransactionEntity
import com.example.chitieuapp.data.receipt.ReceiptStore
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditUiState(
    val date: LocalDate,
    val isEdit: Boolean = false,
    val loading: Boolean = false,
    val isIncome: Boolean = false,
    val amountText: String = "",
    val categoryId: String? = null,
    val note: String = "",
    val receiptPath: String? = null,
    val receiptUrl: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    private val receipts: ReceiptStore,
    private val clock: Clock,
) : ViewModel() {

    /** Tham số "id" của AddEditRoute (null = thêm mới). */
    private val editId: String? = savedStateHandle["id"]

    private val form = MutableStateFlow(
        AddEditUiState(date = LocalDate.now(clock), isEdit = editId != null, loading = editId != null)
    )
    private var existing: TransactionEntity? = null
    private var pendingCapture: Uri? = null

    private val _saved = Channel<Unit>(Channel.BUFFERED)

    /** Phát một lần khi lưu/xoá xong để màn hình quay lại. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    val state: StateFlow<AddEditUiState> = combine(form, categories.observeAll()) { f, cats ->
        f.copy(categories = cats.filter { it.isIncome == f.isIncome })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        editId?.let { id ->
            viewModelScope.launch {
                val tx = transactions.getById(id)
                if (tx == null) {
                    form.update { it.copy(loading = false) }
                } else {
                    existing = tx
                    form.update {
                        it.copy(
                            loading = false,
                            isIncome = tx.isIncome,
                            amountText = tx.amount.toString(),
                            categoryId = tx.categoryId,
                            note = tx.note,
                            date = LocalDate.ofEpochDay(tx.dateEpochDay),
                            receiptPath = tx.receiptLocalPath,
                            receiptUrl = tx.receiptUrl,
                        )
                    }
                }
            }
        }
    }

    fun onTypeChange(isIncome: Boolean) = form.update {
        if (it.isIncome == isIncome) it else it.copy(isIncome = isIncome, categoryId = null, categoryError = null)
    }

    fun onAmountChange(text: String) = form.update {
        it.copy(amountText = text.filter(Char::isDigit).take(MAX_DIGITS), amountError = null)
    }

    fun onCategoryChange(id: String) = form.update { it.copy(categoryId = id, categoryError = null) }
    fun onNoteChange(text: String) = form.update { it.copy(note = text.take(200)) }
    fun onDateChange(date: LocalDate) = form.update { it.copy(date = date) }
    fun onRemoveReceipt() = form.update { it.copy(receiptPath = null, receiptUrl = null) }

    // --- Ảnh hoá đơn ---

    fun createCaptureUri(): Uri = receipts.newCaptureUri().also { pendingCapture = it }

    fun onCaptureResult(success: Boolean) {
        val uri = pendingCapture
        pendingCapture = null
        if (success && uri != null) onImagePicked(uri)
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            receipts.import(uri)?.let { path ->
                // Ảnh mới: xoá URL cũ để SyncWorker upload lại.
                form.update { it.copy(receiptPath = path, receiptUrl = null) }
            }
        }
    }

    // --- Lưu / xoá ---

    fun save() {
        val f = form.value
        val amount = f.amountText.toLongOrNull()
        val amountError = if (amount == null || amount <= 0) "Nhập số tiền hợp lệ" else null
        val categoryError = if (f.categoryId == null) "Chọn danh mục" else null
        if (amountError != null || categoryError != null) {
            form.update { it.copy(amountError = amountError, categoryError = categoryError) }
            return
        }

        form.update { it.copy(saving = true) }
        viewModelScope.launch {
            val now = clock.millis()
            transactions.save(
                TransactionEntity(
                    id = editId ?: UUID.randomUUID().toString(),
                    amount = amount!!,
                    isIncome = f.isIncome,
                    categoryId = f.categoryId!!,
                    note = f.note.trim(),
                    dateEpochDay = f.date.toEpochDay(),
                    receiptLocalPath = f.receiptPath,
                    receiptUrl = f.receiptUrl,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
            _saved.send(Unit)
        }
    }

    fun delete() {
        val id = editId ?: return
        viewModelScope.launch {
            transactions.delete(id)
            _saved.send(Unit)
        }
    }

    private companion object {
        const val MAX_DIGITS = 12
    }
}
