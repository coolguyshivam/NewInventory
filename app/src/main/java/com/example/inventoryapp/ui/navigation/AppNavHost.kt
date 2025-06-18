package com.example.inventoryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.ui.screens.InventoryScreen
import com.example.inventoryapp.ui.screens.ReportsScreen
import com.example.inventoryapp.ui.screens.TransactionScreen

@Composable
fun AppNavHost(
    authRepo: AuthRepository,
    inventoryRepo: InventoryRepository,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier // add this
) {
    NavHost(
        navController = navController,
        startDestination = "inventory",
        modifier = modifier // <-- use modifier here
    ) {
        composable("inventory") {
            InventoryScreen(navController, inventoryRepo, authRepo)
        }
        composable("reports") {
            ReportsScreen(inventoryRepo)
        }
        composable("transaction") {
            TransactionScreen(navController, inventoryRepo)
        }
    }
}