package com.example.inventoryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.ui.screens.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@Composable
fun AppNavHost(
    authRepo: AuthRepository,
    inventoryRepo: InventoryRepository,
    navController: NavHostController,
    userRole: UserRole,
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
        composable("transaction") {
            TransactionScreen(
                navController = navController,
                inventoryRepo = inventoryRepo,
                userRole = userRole
            )
        }
        composable("barcode_scanner") {
            BarcodeScannerScreen(
                navController = navController
            )
        }
        composable("reports") {
            AnalyticsScreen(inventoryRepo = inventoryRepo)
        }
        composable("transaction_history") {
            TransactionHistoryScreen(
                inventoryRepo = inventoryRepo
            )
        }
        composable("analytics") {
            AnalyticsScreen(inventoryRepo = inventoryRepo)
        }
    }
}

// Simple placeholder for screens not implemented yet
@Composable
fun PlaceholderScreen(name: String) {
    Text(
        text = "$name Screen Coming Soon!",
        modifier = Modifier.padding(32.dp)
    )
}