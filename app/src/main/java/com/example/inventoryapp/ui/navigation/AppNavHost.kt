package com.example.inventoryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.ui.screens.*

@Composable
fun AppNavHost(authRepo: AuthRepository, inventoryRepo: InventoryRepository) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController, authRepo) }
        composable("login") { LoginScreen(navController, authRepo) }
        composable("register") { RegisterScreen(navController, authRepo) }
        composable("inventory") { InventoryScreen(navController, inventoryRepo, authRepo) }
        composable("addEditItem/{itemId?}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            AddEditItemScreen(navController, inventoryRepo, itemId)
        }
        composable("transaction/{serial}") { backStackEntry ->
            val serial = backStackEntry.arguments?.getString("serial") ?: ""
            TransactionScreen(navController, inventoryRepo, serial)
        }
    }
}