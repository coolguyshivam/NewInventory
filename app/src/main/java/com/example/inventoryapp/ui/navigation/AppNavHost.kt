package com.example.inventoryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.ui.screens.InventoryScreen
import com.example.inventoryapp.ui.screens.ReportsScreen
import com.example.inventoryapp.ui.screens.TransactionScreen
import com.example.inventoryapp.ui.screens.BarcodeScannerScreen

@Composable
fun AppNavHost(
    authRepo: AuthRepository,
    inventoryRepo: InventoryRepository,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "inventory",
        modifier = modifier
    ) {
        composable("inventory") {
            InventoryScreen(navController, inventoryRepo)
        }
        composable("reports") {
            ReportsScreen(navController, inventoryRepo)
        }
        composable("transaction") {
            TransactionScreen(navController, inventoryRepo)
        }
        composable("barcode_scanner") {
            BarcodeScannerScreen(navController)
        }
    }
}