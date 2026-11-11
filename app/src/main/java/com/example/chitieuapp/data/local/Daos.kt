package com.example.chitieuapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    /** Giao dịch cần đẩy lên: chưa sync, hoặc có ảnh hoá đơn local nhưng chưa có URL trên Storage. */
    @Query(
        "SELECT * FROM transactions WHERE synced = 0 OR " +
            "(deleted = 0 AND receiptLocalPath IS NOT NULL AND receiptUrl IS NULL)"
    )
    suspend fun getPending(): List<TransactionEntity>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE deleted = 0 AND isIncome = 0 AND dateEpochDay BETWEEN :start AND :end"
    )
    suspend fun sumExpense(start: Long, end: Long): Long

    @Upsert
    suspend fun upsert(entity: TransactionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TransactionEntity>)

    /** Chỉ đánh dấu đã sync nếu bản ghi chưa bị sửa trong lúc đang đồng bộ (so updatedAt). */
    @Query("UPDATE transactions SET synced = 1, receiptUrl = :receiptUrl WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long, receiptUrl: String?)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY isIncome, isDefault DESC, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE synced = 0 AND isDefault = 0")
    suspend fun getPending(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entities: List<CategoryEntity>)

    @Upsert
    suspend fun upsert(entity: CategoryEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CategoryEntity>)

    @Query("UPDATE categories SET synced = 1 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun observe(month: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month")
    suspend fun getByMonth(month: String): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE synced = 0")
    suspend fun getPending(): List<BudgetEntity>

    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BudgetEntity>)

    @Query("UPDATE budgets SET synced = 1 WHERE month = :month AND updatedAt = :updatedAt")
    suspend fun markSynced(month: String, updatedAt: Long)
}
