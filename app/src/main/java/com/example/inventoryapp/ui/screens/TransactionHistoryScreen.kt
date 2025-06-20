package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        transactions.clear()
        val result = inventoryRepo.getAllTransactions()
        if (result is Result.Success) {
            transactions.addAll(result.data)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Transaction History") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(transactions.size) { idx ->
                val tx = transactions[idx]
                Card(
                    Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Type: ${tx.type} | Serial: ${tx.serial}")
                        Text("Model: ${tx.model}")
                        Text("Amount: \u20b9${tx.amount}")
                        Text("Date: ${tx.date}")
                        Text("Quantity: ${tx.quantity}")
                        if (tx.description.isNotBlank())
                            Text("Description: ${tx.description}")
                    }
                }
            }
        }
    }
}