package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.InventoryItem
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun InventoryScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    authRepo: AuthRepository
) {
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    // In real usage, use ViewModel or LaunchedEffect for async loading.
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
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        LazyColumn {
            items(inventory) { item ->
                Text("${item.name} (${item.quantity})")
                // Use InventoryCard for nicer UI
            }
        }
    }
}