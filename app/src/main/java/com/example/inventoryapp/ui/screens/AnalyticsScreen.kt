package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository

@Composable
fun AnalyticsScreen(
    inventoryRepo: InventoryRepository
) {
    // Example: show totals for sales and purchases
    val (totalSales, setTotalSales) = remember { mutableStateOf(0.0) }
    val (totalPurchases, setTotalPurchases) = remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        val all = inventoryRepo.getAllTransactions()
        setTotalSales(all.filter { it.type == "Sale" }.sumOf { it.amount })
        setTotalPurchases(all.filter { it.type == "Purchase" }.sumOf { it.amount })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Analytics/Stats") })
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Total Sales: \u20b9$totalSales", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text("Total Purchases: \u20b9$totalPurchases", style = MaterialTheme.typography.titleLarge)
            // Add more charts and analytics as needed...
        }
    }
}