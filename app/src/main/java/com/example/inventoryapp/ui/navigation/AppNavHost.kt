package com.example.inventoryapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.ui.screens.*
import androidx.compose.foundation.layout.padding

sealed class MainScreen(val route: String, val label: String, val icon: ImageVector) {
    object Inventory : MainScreen(
        "inventory",
        "Inventory",
        Icons.AutoMirrored.Filled.List
    )
    object Transaction : MainScreen(
        "transaction",
        "Transaction",
        Icons.Filled.SwapHoriz
    )
    object Analytics : MainScreen(
        "analytics",
        "Analytics",
        Icons.Filled.ShowChart
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
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
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
                InventoryScreen(navController, inventoryRepo)
            }
            composable(MainScreen.Transaction.route) {
                showBottomBar = true
                // Pass the actual userRole if needed by TransactionScreen
                TransactionScreen(navController, inventoryRepo, userRole)
            }
            if (userRole == UserRole.ADMIN) {
                composable(MainScreen.Analytics.route) {
                    showBottomBar = true
                    AnalyticsScreen(inventoryRepo)
                }
            }
            composable(
                route = "transaction_screen?type={type}&serial={serial}&model={model}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = ""; nullable = true },
                    navArgument("serial") { type = NavType.StringType; defaultValue = ""; nullable = true },
                    navArgument("model") { type = NavType.StringType; defaultValue = ""; nullable = true }
                )
            ) { backStackEntry ->
                showBottomBar = false
                // Optionally: use arguments for TransactionScreen if needed
                TransactionScreen(
                    navController,
                    inventoryRepo,
                    userRole
                )
            }
            composable(
                route = "addEditItem/{itemId?}",
                arguments = listOf(
                    navArgument("itemId") { type = NavType.StringType; defaultValue = null; nullable = true }
                )
            ) { backStackEntry ->
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