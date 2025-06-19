package com.example.inventoryapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inventoryapp.model.Transaction

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .let { if (onClick != null) it.clickable { onClick() } else it }

    Card(modifier = cardModifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Type: ${transaction.type}", style = MaterialTheme.typography.titleMedium)
            Text("Model: ${transaction.model}")
            Text("Serial: ${transaction.serial}")
            transaction.phone?.takeIf { it.isNotBlank() }?.let { Text("Phone: $it") }
            transaction.aadhaar?.takeIf { it.isNotBlank() }?.let { Text("Aadhaar: $it") }
            Text("Amount: ₹${transaction.amount}")
            Text("Quantity: ${transaction.quantity}")
            Text("Date: ${transaction.date}")
            transaction.description?.takeIf { it.isNotBlank() }?.let {
                Text("Description: $it")
            }
            if (transaction.imageUrls.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transaction.imageUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Transaction Image",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }
        }
    }
}