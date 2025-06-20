package com.example.inventoryapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.ui.screens.*

sealed class MainScreen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Inventory : MainScreen(
        "inventory",
        "Inventory",
        { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Inventory") }
    )
    object Transaction : MainScreen(
        "transaction",
        "Transaction",
        { Icon(Icons.Filled.AddShoppingCart, contentDescription = "Transaction") }
    )
    object TransactionHistory : MainScreen(
        "transaction_history",
        "Transaction History",
        { Icon(Icons.Filled.Receipt, contentDescription = "Transaction History") }
    )
    object Analytics : MainScreen(
        "analytics",
        "Analytics",
        { Icon(Icons.Filled.ShowChart, contentDescription = "Analytics") }
    )
}

@Composable
fun AppNavHost(
    authRepo: AuthRepository,
    inventoryRepo: InventoryRepository,
    navController: NavHostController,
    userRole: UserRole,
    modifier: Modifier = Modifier
) {
    var showBottomBar by remember { mutableStateOf(true) }

    val mainScreens = buildList {
        add(MainScreen.Inventory)
        add(MainScreen.Transaction)
        add(MainScreen.TransactionHistory)
        if (userRole == UserRole.ADMIN) add(MainScreen.Analytics)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
                    mainScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon() },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().route!!) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                showBottomBar = false
                SplashScreen(navController, authRepo)
            }
            composable("login") {
                showBottomBar = false
                LoginScreen(navController, authRepo)
            }
            composable("register") {
                showBottomBar = false
                RegisterScreen(navController, authRepo)
            }
            composable(MainScreen.Inventory.route) {
                showBottomBar = true
                InventoryScreen(navController, inventoryRepo, authRepo)
            }
            composable(MainScreen.Transaction.route) {
                showBottomBar = true
                TransactionScreen(navController, inventoryRepo, serial = "")
            }
            composable(
                route = "transaction/{serial}",
                arguments = listOf(
                    navArgument("serial") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = false
                    }
                )
            ) { backStackEntry ->
                showBottomBar = false
                val serial = backStackEntry.arguments?.getString("serial") ?: ""
                TransactionScreen(navController, inventoryRepo, serial)
            }
            composable(MainScreen.TransactionHistory.route) {
                showBottomBar = true
                TransactionHistoryScreen(inventoryRepo)
            }
            if (userRole == UserRole.ADMIN) {
                composable(MainScreen.Analytics.route) {
                    showBottomBar = true
                    AnalyticsScreen(inventoryRepo)
                }
            }
            composable("addEditItem/{itemId?}") { backStackEntry ->
                showBottomBar = false
                AddEditItemScreen(
                    navController = navController,
                    inventoryRepo = inventoryRepo,
                    itemId = backStackEntry.arguments?.getString("itemId")
                )
            }
        }
    }
}