package com.example.chitieuapp.data.repository

import com.example.chitieuapp.data.local.BudgetDao
import com.example.chitieuapp.data.local.BudgetEntity
import com.example.chitieuapp.data.local.CategoryDao
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.local.DefaultCategories
import com.example.chitieuapp.data.local.TransactionDao
import com.example.chitieuapp.data.local.TransactionEntity
import com.example.chitieuapp.data.notify.BudgetNotifier
import com.example.chitieuapp.data.sync.SyncScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: mọi thao tác ghi vào Room trước (synced = false), sau đó xếp lịch
 * SyncWorker để đẩy lên Firestore khi có mạng. UI chỉ đọc từ Room.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val scheduler: SyncScheduler,
    private val notifier: BudgetNotifier,
    private val clock: Clock,
) : TransactionRepository {

    override fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()

    override suspend fun getById(id: String): TransactionEntity? =
        dao.getById(id)?.takeIf { !it.deleted }

    override suspend fun save(transaction: TransactionEntity) {
        dao.upsert(transaction.copy(updatedAt = clock.millis(), synced = false, deleted = false))
        scheduler.requestSync()
        if (!transaction.isIncome) {
            notifier.check(YearMonth.from(LocalDate.ofEpochDay(transaction.dateEpochDay)))
        }
    }

    override suspend fun delete(id: String) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(deleted = true, synced = false, updatedAt = clock.millis()))
        scheduler.requestSync()
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    private val scheduler: SyncScheduler,
    private val clock: Clock,
) : CategoryRepository {

    override fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    override suspend fun ensureDefaults() = dao.insertAllIgnore(DefaultCategories.all)

    override suspend fun save(category: CategoryEntity) {
        dao.upsert(
            category.copy(isDefault = false, updatedAt = clock.millis(), synced = false, deleted = false)
        )
        scheduler.requestSync()
    }

    override suspend fun delete(id: String) {
        val existing = dao.getById(id) ?: return
        if (existing.isDefault) return // danh mục mặc định không được xoá
        dao.upsert(existing.copy(deleted = true, synced = false, updatedAt = clock.millis()))
        scheduler.requestSync()
    }
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao,
    private val scheduler: SyncScheduler,
    private val notifier: BudgetNotifier,
    private val clock: Clock,
) : BudgetRepository {

    override fun observe(month: String): Flow<BudgetEntity?> = dao.observe(month)

    override suspend fun setBudget(month: String, limit: Long) {
        dao.upsert(BudgetEntity(month = month, limitAmount = limit, updatedAt = clock.millis(), synced = false))
        scheduler.requestSync()
        notifier.check(YearMonth.parse(month))
    }
}
