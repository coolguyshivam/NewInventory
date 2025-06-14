package com.example.inventoryapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.model.InventoryItem

@Composable
fun InventoryCard(
    item: InventoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text("Quantity: ${item.quantity}")
            if (item.description.isNotBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}