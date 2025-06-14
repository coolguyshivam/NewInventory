package com.example.inventoryapp.ui.components

import androidx.compose.runtime.Composable
import com.example.inventoryapp.model.InventoryItem
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun InventoryCard(item: InventoryItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text("Quantity: ${item.quantity}")
            item.description.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}