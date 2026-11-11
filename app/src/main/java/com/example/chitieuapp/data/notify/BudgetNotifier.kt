package com.example.chitieuapp.data.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.chitieuapp.data.local.BudgetDao
import com.example.chitieuapp.data.local.TransactionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thông báo local khi chi tiêu tháng đạt 80% / 100% ngân sách.
 * Mỗi mốc của mỗi tháng chỉ báo một lần (lưu cờ trong SharedPreferences).
 * Chỉ hiển thị nếu người dùng đã cấp quyền POST_NOTIFICATIONS (Android 13+).
 */
@Singleton
class BudgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
) {
    private val prefs by lazy { context.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE) }

    suspend fun check(month: YearMonth) {
        val budget = budgetDao.getByMonth(month.toString()) ?: return
        if (budget.limitAmount <= 0) return

        val spent = transactionDao.sumExpense(
            start = month.atDay(1).toEpochDay(),
            end = month.atEndOfMonth().toEpochDay(),
        )
        val percent = spent * 100 / budget.limitAmount
        val level = when {
            percent >= 100 -> 100
            percent >= 80 -> 80
            else -> return
        }

        val flag = "alert_${month}_$level"
        if (prefs.getBoolean(flag, false)) return
        if (!canNotify()) return

        val (title, text) = if (level == 100) {
            "Đã vượt ngân sách tháng ${month.monthValue}" to
                "Bạn đã chi ${money(spent)} / ${money(budget.limitAmount)}."
        } else {
            "Sắp hết ngân sách tháng ${month.monthValue}" to
                "Bạn đã dùng $percent% ngân sách (${money(spent)} / ${money(budget.limitAmount)})."
        }
        show(level, title, text)
        prefs.edit().putBoolean(flag, true).apply()
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission") // đã kiểm tra trong canNotify()
    private fun show(level: Int, title: String, text: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Cảnh báo ngân sách", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(level, notification)
    }

    private fun money(v: Long) =
        NumberFormat.getInstance(Locale.forLanguageTag("vi-VN")).format(v) + " ₫"

    private companion object {
        const val CHANNEL_ID = "budget_alerts"
    }
}
