package com.example.chitieuapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chitieuapp.data.repository.AuthState
import com.example.chitieuapp.data.repository.AuthUser
import com.example.chitieuapp.ui.MainViewModel
import com.example.chitieuapp.ui.addedit.AddEditScreen
import com.example.chitieuapp.ui.budget.BudgetScreen
import com.example.chitieuapp.ui.dashboard.DashboardScreen
import com.example.chitieuapp.ui.login.LoginScreen
import com.example.chitieuapp.ui.stats.StatsScreen
import com.example.chitieuapp.ui.transactions.TransactionsScreen

private data class TopLevelTab(val route: Any, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TopLevelTab(DashboardRoute, "Tổng quan", Icons.Filled.Home),
    TopLevelTab(TransactionsRoute, "Giao dịch", Icons.AutoMirrored.Filled.List),
    TopLevelTab(StatsRoute, "Thống kê", Icons.Filled.PieChart),
    TopLevelTab(BudgetRoute, "Ngân sách", Icons.Filled.AccountBalanceWallet),
)

@Composable
fun ChiTieuApp(viewModel: MainViewModel = hiltViewModel()) {
    val auth by viewModel.authState.collectAsStateWithLifecycle()
    when (val state = auth) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> AppContent(
            isSignedIn = state is AuthState.SignedIn,
            user = (state as? AuthState.SignedIn)?.user,
            onSignOut = viewModel::signOut,
        )
    }
}

@Composable
private fun AppContent(isSignedIn: Boolean, user: AuthUser?, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val startDestination: Any = remember { if (isSignedIn) DashboardRoute else LoginRoute }

    LaunchedEffect(isSignedIn) {
        val current = navController.currentDestination ?: return@LaunchedEffect
        val onLogin = current.hasRoute<LoginRoute>()
        if (isSignedIn && onLogin) {
            navController.navigate(DashboardRoute) { popUpTo<LoginRoute> { inclusive = true } }
        } else if (!isSignedIn && !onLogin) {
            navController.navigate(LoginRoute) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(DashboardRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<LoginRoute> { LoginScreen() }

            composable<DashboardRoute> {
                DashboardScreen(
                    user = user,
                    onSignOut = onSignOut,
                    onAddTransaction = { navController.navigate(AddEditRoute()) },
                    onOpenTransaction = { id -> navController.navigate(AddEditRoute(id)) },
                    onSeeAll = { navController.navigate(TransactionsRoute) },
                    onOpenBudget = { navController.navigate(BudgetRoute) },
                )
            }
            composable<TransactionsRoute> {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(AddEditRoute()) },
                    onOpenTransaction = { id -> navController.navigate(AddEditRoute(id)) },
                )
            }
            composable<StatsRoute> { StatsScreen() }
            composable<BudgetRoute> { BudgetScreen() }
            composable<AddEditRoute> { AddEditScreen(onDone = { navController.popBackStack() }) }
        }
    }
}
