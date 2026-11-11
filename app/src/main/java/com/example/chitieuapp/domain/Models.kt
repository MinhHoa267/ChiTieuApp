package com.example.chitieuapp.domain

import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.local.TransactionEntity
import java.time.YearMonth

/** Giao dịch kèm tên/màu danh mục để hiển thị trong danh sách. */
data class TransactionItem(
    val tx: TransactionEntity,
    val categoryName: String,
    val categoryColor: Long,
)

const val UNKNOWN_CATEGORY_NAME = "Khác"
const val UNKNOWN_CATEGORY_COLOR = 0xFF9E9E9EL

fun YearMonth.key(): String = toString() // "2026-11"

fun TransactionEntity.inMonth(month: YearMonth): Boolean =
    dateEpochDay in month.atDay(1).toEpochDay()..month.atEndOfMonth().toEpochDay()

fun List<TransactionEntity>.sortedForDisplay(): List<TransactionEntity> =
    sortedWith(
        compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.createdAt }
    )

fun List<TransactionEntity>.toItems(categories: List<CategoryEntity>): List<TransactionItem> {
    val byId = categories.associateBy { it.id }
    return map { tx ->
        val category = byId[tx.categoryId]
        TransactionItem(
            tx = tx,
            categoryName = category?.name ?: UNKNOWN_CATEGORY_NAME,
            categoryColor = category?.colorArgb ?: UNKNOWN_CATEGORY_COLOR,
        )
    }
}
