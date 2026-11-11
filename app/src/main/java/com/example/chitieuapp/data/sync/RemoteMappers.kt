package com.example.chitieuapp.data.sync

import com.example.chitieuapp.data.local.BudgetEntity
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.local.TransactionEntity
import com.google.firebase.firestore.DocumentSnapshot

/*
 * Dùng Map thay vì để Firestore tự (de)serialize data class bằng reflection,
 * nhờ vậy bản release bật R8 không bị đổi tên field làm hỏng dữ liệu.
 */

fun TransactionEntity.toMap(): Map<String, Any?> = mapOf(
    "amount" to amount,
    "isIncome" to isIncome,
    "categoryId" to categoryId,
    "note" to note,
    "dateEpochDay" to dateEpochDay,
    "receiptUrl" to receiptUrl,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toTransaction(): TransactionEntity? = TransactionEntity(
    id = id,
    amount = getLong("amount") ?: return null,
    isIncome = getBoolean("isIncome") ?: false,
    categoryId = getString("categoryId") ?: return null,
    note = getString("note") ?: "",
    dateEpochDay = getLong("dateEpochDay") ?: return null,
    receiptUrl = getString("receiptUrl"),
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getLong("updatedAt") ?: 0L,
    synced = true,
)

fun CategoryEntity.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "isIncome" to isIncome,
    "colorArgb" to colorArgb,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toCategory(): CategoryEntity? = CategoryEntity(
    id = id,
    name = getString("name") ?: return null,
    isIncome = getBoolean("isIncome") ?: false,
    colorArgb = getLong("colorArgb") ?: 0xFF9E9E9EL,
    isDefault = false,
    updatedAt = getLong("updatedAt") ?: 0L,
    synced = true,
)

fun BudgetEntity.toMap(): Map<String, Any?> = mapOf(
    "limitAmount" to limitAmount,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toBudget(): BudgetEntity? = BudgetEntity(
    month = id,
    limitAmount = getLong("limitAmount") ?: return null,
    updatedAt = getLong("updatedAt") ?: 0L,
    synced = true,
)
