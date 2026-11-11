package com.example.chitieuapp.data.sync

import android.net.Uri
import com.example.chitieuapp.data.local.BudgetDao
import com.example.chitieuapp.data.local.CategoryDao
import com.example.chitieuapp.data.local.TransactionDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Đồng bộ hai chiều Room <-> Cloud Firestore (users/{uid}/...), ảnh hoá đơn -> Cloud Storage.
 *
 * Quy tắc: (1) đẩy mọi thay đổi cục bộ chưa sync, (2) kéo toàn bộ dữ liệu từ server,
 * bản ghi nào server mới hơn (updatedAt) và local không có thay đổi chờ đẩy thì ghi đè local.
 * Bản ghi local đã sync mà server không còn => đã bị xoá ở thiết bị khác => xoá local.
 */
@Singleton
class SyncManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
) {
    private val mutex = Mutex()

    suspend fun sync() = mutex.withLock {
        val uid = auth.currentUser?.uid ?: return@withLock
        val userDoc = firestore.collection("users").document(uid)
        syncCategories(userDoc)
        syncBudgets(userDoc)
        syncTransactions(uid, userDoc)
    }

    private suspend fun syncCategories(userDoc: DocumentReference) {
        val col = userDoc.collection("categories")
        for (c in categoryDao.getPending()) {
            if (c.deleted) {
                col.document(c.id).delete().await()
                categoryDao.deleteById(c.id)
            } else {
                col.document(c.id).set(c.toMap()).await()
                categoryDao.markSynced(c.id, c.updatedAt)
            }
        }
        val remote = col.get(Source.SERVER).await().documents.mapNotNull { it.toCategory() }
        val remoteIds = remote.map { it.id }.toSet()
        val local = categoryDao.getAll().associateBy { it.id }
        categoryDao.upsertAll(
            remote.filter { r -> local[r.id].let { l -> l == null || (l.synced && r.updatedAt > l.updatedAt) } }
        )
        local.values
            .filter { it.synced && !it.isDefault && !it.deleted && it.id !in remoteIds }
            .forEach { categoryDao.deleteById(it.id) }
    }

    private suspend fun syncBudgets(userDoc: DocumentReference) {
        val col = userDoc.collection("budgets")
        for (b in budgetDao.getPending()) {
            col.document(b.month).set(b.toMap()).await()
            budgetDao.markSynced(b.month, b.updatedAt)
        }
        val remote = col.get(Source.SERVER).await().documents.mapNotNull { it.toBudget() }
        val local = budgetDao.getAll().associateBy { it.month }
        budgetDao.upsertAll(
            remote.filter { r -> local[r.month].let { l -> l == null || (l.synced && r.updatedAt > l.updatedAt) } }
        )
    }

    private suspend fun syncTransactions(uid: String, userDoc: DocumentReference) {
        val col = userDoc.collection("transactions")

        // 1) Đẩy lên
        for (t in transactionDao.getPending()) {
            if (t.deleted) {
                col.document(t.id).delete().await()
                runCatching { storage.reference.child(receiptPath(uid, t.id)).delete().await() }
                transactionDao.deleteById(t.id)
                continue
            }
            var url = t.receiptUrl
            val localPath = t.receiptLocalPath
            if (url == null && localPath != null) {
                // Upload ảnh thất bại (ví dụ project Spark chưa bật Storage) thì vẫn sync dữ liệu,
                // ảnh sẽ được thử lại ở lần sync sau.
                url = runCatching { uploadReceipt(uid, t.id, File(localPath)) }.getOrNull()
            }
            col.document(t.id).set(t.copy(receiptUrl = url).toMap()).await()
            transactionDao.markSynced(t.id, t.updatedAt, url)
        }

        // 2) Kéo về (Source.SERVER: offline sẽ ném lỗi thay vì trả cache thiếu làm xoá nhầm)
        val remote = col.get(Source.SERVER).await().documents.mapNotNull { it.toTransaction() }
        val remoteIds = remote.map { it.id }.toSet()
        val local = transactionDao.getAll().associateBy { it.id }

        transactionDao.upsertAll(
            remote
                .filter { r -> local[r.id].let { l -> l == null || (l.synced && r.updatedAt > l.updatedAt) } }
                .map { r -> r.copy(receiptLocalPath = local[r.id]?.receiptLocalPath, synced = true) }
        )
        local.values
            .filter { it.synced && !it.deleted && it.id !in remoteIds }
            .forEach { transactionDao.deleteById(it.id) }
    }

    private suspend fun uploadReceipt(uid: String, id: String, file: File): String? {
        if (!file.exists()) return null
        val ref = storage.reference.child(receiptPath(uid, id))
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    private fun receiptPath(uid: String, id: String) = "users/$uid/receipts/$id.jpg"
}
