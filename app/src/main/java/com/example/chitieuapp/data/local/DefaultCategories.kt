package com.example.chitieuapp.data.local

object DefaultCategories {
    val all: List<CategoryEntity> = listOf(
        // Chi tiêu
        c("food", "Ăn uống", false, 0xFFEF5350),
        c("transport", "Di chuyển", false, 0xFF42A5F5),
        c("shopping", "Mua sắm", false, 0xFFAB47BC),
        c("bills", "Hoá đơn", false, 0xFFFFA726),
        c("health", "Sức khoẻ", false, 0xFF26A69A),
        c("entertainment", "Giải trí", false, 0xFFEC407A),
        c("education", "Học tập", false, 0xFF5C6BC0),
        c("other_expense", "Khác", false, 0xFF8D6E63),
        // Thu nhập
        c("salary", "Lương", true, 0xFF66BB6A),
        c("bonus", "Thưởng", true, 0xFFD4E157),
        c("other_income", "Thu nhập khác", true, 0xFF26C6DA),
    )

    private fun c(id: String, name: String, isIncome: Boolean, color: Long) = CategoryEntity(
        id = id,
        name = name,
        isIncome = isIncome,
        colorArgb = color,
        isDefault = true,
        updatedAt = 0L,
        synced = true,
    )
}
