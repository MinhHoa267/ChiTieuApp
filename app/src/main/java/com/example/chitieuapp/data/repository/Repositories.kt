package com.example.chitieuapp.data.repository

import com.example.chitieuapp.data.local.BudgetEntity
import com.example.chitieuapp.data.local.CategoryEntity
import com.example.chitieuapp.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** ViewModel chỉ làm việc với các interface này (dễ thay bằng fake khi test). */
interface TransactionRepository {
    fun observeAll(): Flow<List<TransactionEntity>>
    suspend fun getById(id: String): TransactionEntity?
    suspend fun save(transaction: TransactionEntity)
    suspend fun delete(id: String)
}

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    suspend fun ensureDefaults()
    suspend fun save(category: CategoryEntity)
    suspend fun delete(id: String)
}

interface BudgetRepository {
    fun observe(month: String): Flow<BudgetEntity?>
    suspend fun setBudget(month: String, limit: Long)
}

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

data class AuthUser(
    val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?,
)
