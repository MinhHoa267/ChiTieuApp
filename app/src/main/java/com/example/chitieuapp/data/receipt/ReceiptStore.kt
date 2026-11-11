package com.example.chitieuapp.data.receipt

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Quản lý ảnh hoá đơn. Tách interface để ViewModel test được mà không cần Context. */
interface ReceiptStore {
    /** Uri (FileProvider) để app camera ghi ảnh vào, không cần quyền CAMERA. */
    fun newCaptureUri(): Uri

    /** Sao chép ảnh (từ camera hoặc thư viện) vào bộ nhớ riêng của app, trả về đường dẫn file. */
    suspend fun import(uri: Uri): String?
}

@Singleton
class ReceiptFiles @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReceiptStore {

    override fun newCaptureUri(): Uri {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "capture_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    override suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "receipts").apply { mkdirs() }
            val dest = File(dir, "${UUID.randomUUID()}.jpg")
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            input.use { source -> dest.outputStream().use { source.copyTo(it) } }
            dest.absolutePath
        }.getOrNull()
    }
}
