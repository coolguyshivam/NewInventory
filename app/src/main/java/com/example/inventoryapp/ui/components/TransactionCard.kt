package com.example.inventoryapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.model.Transaction

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val cardColor = when (transaction.type.lowercase()) {
        "purchase" -> Color(0xFFB9F6CA) // Green
        "sale" -> Color(0xFFB3E5FC)     // Blue
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Type: ${transaction.type}", style = MaterialTheme.typography.titleMedium)
            Text("Model: ${transaction.model}")
            Text("Serial: ${transaction.serial}")
            Text("Amount: ${transaction.amount}")
            Text("Date: ${transaction.date}")
            transaction.description.takeIf { it.isNotBlank() }?.let {
                Text("Description: $it")
            }
        }
    }
}