package com.example.chitieuapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Danh mục thu/chi. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isIncome: Boolean,
    val colorArgb: Long,
    val isDefault: Boolean = false,
    val updatedAt: Long = 0L,
    val synced: Boolean = true,
    val deleted: Boolean = false,
)

/**
 * Giao dịch thu/chi.*/
@Entity(tableName = "transactions", indices = [Index("dateEpochDay")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val isIncome: Boolean,
    val categoryId: String,
    val note: String = "",
    val dateEpochDay: Long,
    val receiptLocalPath: String? = null,
    val receiptUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
    val deleted: Boolean = false,
)

/** Ngân sách theo tháng. month có dạng "yyyy-MM" (ví dụ "2026-11"). */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val month: String,
    val limitAmount: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
)
