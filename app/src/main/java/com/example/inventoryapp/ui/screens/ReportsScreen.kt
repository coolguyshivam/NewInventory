package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (transactions.isEmpty()) {
                Text("No transactions available.")
            } else {
                // For each date (string), show a header and a list of transactions for that date
                transactionsByDate.forEach { (date, txList) ->
                    Text("Date: $date", style = MaterialTheme.typography.titleMedium)
                    // For each transaction on this date, show details
                    txList.forEach { tx ->
                        Text("Serial: ${tx.serial} | Type: ${tx.type} | Model: ${tx.model} | Amount: ${tx.amount}")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}