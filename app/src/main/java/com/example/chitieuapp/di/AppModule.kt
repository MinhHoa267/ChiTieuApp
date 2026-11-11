package com.example.chitieuapp.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.room.Room
import com.example.chitieuapp.data.auth.AuthRepository
import com.example.chitieuapp.data.auth.AuthRepositoryImpl
import com.example.chitieuapp.data.local.AppDatabase
import com.example.chitieuapp.data.local.BudgetDao
import com.example.chitieuapp.data.local.CategoryDao
import com.example.chitieuapp.data.local.TransactionDao
import com.example.chitieuapp.data.receipt.ReceiptFiles
import com.example.chitieuapp.data.receipt.ReceiptStore
import com.example.chitieuapp.data.repository.BudgetRepository
import com.example.chitieuapp.data.repository.BudgetRepositoryImpl
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.repository.CategoryRepositoryImpl
import com.example.chitieuapp.data.repository.TransactionRepository
import com.example.chitieuapp.data.repository.TransactionRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "chitieu.db").build()

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    @Provides @Singleton fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides @Singleton fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {
    @Binds @Singleton abstract fun bindTransactions(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindCategories(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindBudgets(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindAuth(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindReceipts(impl: ReceiptFiles): ReceiptStore
}
