package com.example.chitieuapp.ui.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VIETNAM = Locale.forLanguageTag("vi-VN")

/** 1500000 -> "1.500.000 ₫" */
fun formatMoney(amount: Long): String =
    NumberFormat.getInstance(VIETNAM).format(amount) + " ₫"

fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun formatDateHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Hôm nay"
    today.minusDays(1) -> "Hôm qua"
    else -> formatDate(date)
}

/** Tìm Activity từ Context (LocalContext có thể bị bọc bởi ContextWrapper). */
fun Context.findActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
