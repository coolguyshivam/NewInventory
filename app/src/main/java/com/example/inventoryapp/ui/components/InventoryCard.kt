package com.example.inventoryapp.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import com.example.inventoryapp.model.InventoryItem
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCard(item: InventoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Model: ${item.model}")
            Text("Serial: ${item.serial}")
            Text("Date: ${item.date}")
        }
    }
}