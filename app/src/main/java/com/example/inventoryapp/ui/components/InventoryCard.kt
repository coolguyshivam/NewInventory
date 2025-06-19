package com.example.inventoryapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.model.InventoryItem

@Composable
fun InventoryCard(
    item: InventoryItem,
    modifier: Modifier = Modifier,
    onSell: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Model: ${item.model}", style = MaterialTheme.typography.bodyMedium)
            Text("Serial: ${item.serial}", style = MaterialTheme.typography.bodyMedium)
            Text("Quantity: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
            if (item.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            if (onSell != null) {
                Button(onClick = onSell) {
                    Text("Sell")
                }
            }
        }
    }
}