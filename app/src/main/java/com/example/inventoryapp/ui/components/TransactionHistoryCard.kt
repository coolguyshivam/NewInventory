package com.example.inventoryapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.inventoryapp.model.Transaction

@Composable
fun TransactionHistoryCard(
    transaction: Transaction,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Model: ${transaction.model} | Serial: ${transaction.serial}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (transaction.type.uppercase() == "DELETE") {
                // Special formatting for deletion transactions
                Text("Type: ITEM DELETED", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Text("Date: ${transaction.date}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                transaction.deletedInfo?.let { deletedInfo ->
                    Text("Deleted by: ${deletedInfo.deletedBy}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Deleted at: ${deletedInfo.deletedAt}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                if (transaction.description.isNotBlank()) {
                    Text("Item: ${transaction.description}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            } else {
                Text("Type: ${transaction.type} | Amount: ${transaction.amount}", style = MaterialTheme.typography.bodySmall)
                Text("Date: ${transaction.date}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}