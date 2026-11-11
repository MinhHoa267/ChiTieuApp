package com.example.chitieuapp.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe routes (Navigation Compose 2.8+): mỗi route là một object/data class @Serializable. */
@Serializable data object LoginRoute
@Serializable data object DashboardRoute
@Serializable data object TransactionsRoute
@Serializable data object StatsRoute
@Serializable data object BudgetRoute

/** id = null: thêm mới; id != null: sửa giao dịch. */
@Serializable data class AddEditRoute(val id: String? = null)
