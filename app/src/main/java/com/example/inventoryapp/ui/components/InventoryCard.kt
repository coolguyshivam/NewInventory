package com.example.inventoryapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.inventoryapp.model.InventoryItem

@Composable
fun InventoryCard(item: InventoryItem) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Model: ${item.model}")
            Text("Serial: ${item.serial}")
            Text("Date: ${item.date}")
            Row {
                item.imageUrls.take(3).forEach {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).padding(4.dp)
                    )
                }
            }
        }
    }
}