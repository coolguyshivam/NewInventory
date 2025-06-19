package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.ui.components.InventoryCard

@Composable
fun InventoryScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    authRepo: AuthRepository
) {
    val inventory by inventoryRepo.getInventoryFlow().collectAsState(initial = emptyList()) // Use Flow from repo!

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (inventory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(inventory) { item ->
                    InventoryCard(item = item)
                }
            }
        }
    }
}