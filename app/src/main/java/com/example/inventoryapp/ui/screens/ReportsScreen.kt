package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.ui.components.TransactionCard

@Composable
fun ReportsScreen(inventoryRepo: InventoryRepository) {
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn {
                items(transactions) { tx ->
                    TransactionCard(transaction = tx, onClick = { selectedTransaction = tx })
                }
            }
        }
    }
    selectedTransaction?.let { tx ->
        TransactionDetailDialog(transaction = tx, onDismiss = { selectedTransaction = null })
    }
}