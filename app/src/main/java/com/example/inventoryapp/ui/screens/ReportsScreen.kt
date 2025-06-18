package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import com.example.inventoryapp.data.Result
import java.util.Date

@Composable
fun ReportsScreen(inventoryRepo: InventoryRepository) {
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        when (val result = inventoryRepo.getAllTransactions()) {
            is Result.Success -> {
                transactions = result.data
                error = null
            }
            is Result.Error -> {
                error = result.exception.message
            }
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn {
                items(transactions) { tx ->
                    val color = when (tx.type) {
                        "Sale" -> Color(0xFFD0F8CE)
                        "Purchase" -> Color(0xFFFFF9C4)
                        else -> Color.LightGray
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(color)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Type: ${tx.type}", style = MaterialTheme.typography.titleMedium)
                            Text("Model: ${tx.model}")
                            Text("Serial: ${tx.serial}")
                            Text("Amount: ${tx.amount}")
                            Text("Date: ${tx.date}")
                            Text("Timestamp: ${Date(tx.timestamp)}")
                        }
                    }
                }
            }
        }
    }
}