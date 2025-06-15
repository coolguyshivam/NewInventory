package com.example.inventoryapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.automirrored.filled.List // <-- updated import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.ui.screens.*

sealed class MainScreen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Inventory : MainScreen(
        "inventory",
        "Inventory",
        { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Inventory") } // <-- updated usage
    )
    object Transaction : MainScreen(
        "transaction",
        "Transaction",
        { Icon(Icons.Filled.AddShoppingCart, contentDescription = "Transaction") }
    )
    object Reports : MainScreen(
        "reports",
        "Reports",
        { Icon(Icons.Filled.Assessment, contentDescription = "Reports") }
    )
}

@Composable
fun AppNavHost(authRepo: AuthRepository, inventoryRepo: InventoryRepository) {
    val navController = rememberNavController()
    var showBottomBar by remember { mutableStateOf(true) }

    val mainScreens = listOf(
        MainScreen.Inventory,
        MainScreen.Transaction,
        MainScreen.Reports
    )

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
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                showBottomBar = false
                val serial = backStackEntry.arguments?.getString("serial") ?: ""
                TransactionScreen(navController, inventoryRepo, serial)
            }
            composable(MainScreen.Reports.route) {
                showBottomBar = true
                ReportsScreen(inventoryRepo)
            }
            composable("addEditItem/{itemId?}") { backStackEntry ->
                showBottomBar = false
                val itemId = backStackEntry.arguments?.getString("itemId")
                AddEditItemScreen(navController, inventoryRepo, itemId)
            }
            composable("barcode_scan") {
                showBottomBar = false
                BarcodeScannerScreen(
                    navController = navController,
                    onScanned = { scannedSerial ->
                        navController.popBackStack()
                        navController.navigate("transaction/$scannedSerial")
                    }
                )
            }
        }
    }
}