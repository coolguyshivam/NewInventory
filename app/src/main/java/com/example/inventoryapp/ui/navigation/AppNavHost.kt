package com.example.inventoryapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authRepo: AuthRepository,
    inventoryRepo: InventoryRepository
) {
    val items = inventoryRepo.getInventory()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📦 Inventory Mock Screen", style = MaterialTheme.typography.headlineMedium)

            items.forEach {
                Text("• ${it.name} [${it.serial}]", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { /* Future action */ }) {
                Text("Scan Barcode (Disabled)")
            }
        }
    }
}
