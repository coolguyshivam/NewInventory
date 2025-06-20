package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    inventoryRepo: InventoryRepository
) {
    val transactions = remember { mutableStateListOf<Transaction>() }

    // Load transactions on first composition
    LaunchedEffect(Unit) {
        val result = inventoryRepo.getAllTransactions()
        if (result is Result.Success) {
            transactions.clear()
            transactions.addAll(result.data)
        }
    }

    // Group transactions by date (String)
    val transactionsByDate: Map<String, List<Transaction>> = transactions.groupBy { it.date }

    // Define a color for each category
    fun typeColor(type: String): Color = when (type.lowercase()) {
        "sale" -> Color(0xFFB2FF59)        // Light Green
        "purchase" -> Color(0xFF81D4FA)    // Light Blue
        "return" -> Color(0xFFFFF176)      // Yellow
        "repair" -> Color(0xFFFF8A65)      // Orange
        else -> Color(0xFFE0E0E0)          // Grey
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Transaction History") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (transactions.isEmpty()) {
                Text("No transactions available.")
            } else {
                // For each date, show a header and a list of transactions for that date
                transactionsByDate.forEach { (date, txList) ->
                    Text("Date: $date", style = MaterialTheme.typography.titleMedium)
                    // For each transaction on this date, show details in colored card
                    txList.forEach { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = typeColor(tx.type ?: "")
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Serial: ${tx.serial} | Type: ${tx.type} | Model: ${tx.model} | Amount: ${tx.amount}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}