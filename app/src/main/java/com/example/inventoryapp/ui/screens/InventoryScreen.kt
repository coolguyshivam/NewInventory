package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.ui.components.InventoryCard

@Composable
fun InventoryScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    authRepo: AuthRepository
) {
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        when (val result = inventoryRepo.getInventory()) {
            is Result.Success -> {
                inventory = result.data
                error = null
            }
            is Result.Error -> {
                error = result.exception.message
            }
        }
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn {
                items(inventory) { item ->
                    InventoryCard(item = item, onClick = { selectedItem = item })
                }
            }
        }

        // Details Dialog
        selectedItem?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItem = null },
                confirmButton = {
                    TextButton(onClick = { selectedItem = null }) {
                        Text("Close")
                    }
                },
                title = { Text("Item Details") },
                text = {
                    Column {
                        Text("Name: ${item.name}")
                        Text("Quantity: ${item.quantity}")
                        Text("Description: ${item.description}")
                        // Add other relevant fields if needed
                    }
                }
            )
        }
    }
}