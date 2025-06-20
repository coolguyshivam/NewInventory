package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    inventoryRepo: InventoryRepository
) {
    val (totalSales, setTotalSales) = remember { mutableStateOf(0.0) }
    val (totalPurchases, setTotalPurchases) = remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        val result = inventoryRepo.getAllTransactions()
        if (result is Result.Success) {
            setTotalSales(result.data.filter { it.type == "Sale" }.sumOf { it.amount })
            setTotalPurchases(result.data.filter { it.type == "Purchase" }.sumOf { it.amount })
        }
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
        }
    }
}