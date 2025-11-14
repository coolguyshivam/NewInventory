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
            Text("Type: ${transaction.type} | Amount: ${transaction.amount}", style = MaterialTheme.typography.bodySmall)
            if (transaction.customerName.isNotBlank()) {
                Text("Customer: ${transaction.customerName}", style = MaterialTheme.typography.bodySmall)
            }
            if (transaction.phoneNumber?.isNotBlank() == true) {
                Text("Phone: ${transaction.phoneNumber}", style = MaterialTheme.typography.bodySmall)
            }
            if (transaction.aadhaarNumber?.isNotBlank() == true) {
                Text("Aadhaar: ${transaction.aadhaarNumber}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Date: ${transaction.date}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
            
            // Show deletion info for DELETE transactions
            if (transaction.type.uppercase() == "DELETE" && transaction.deletedInfo != null) {
                Text(
                    "Deleted by: ${transaction.deletedInfo.deletedBy} at ${transaction.deletedInfo.deletedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backgroundColor == Color(0xFFE53E3E)) Color.White else Color.Red
                )
            }
        }
    }
}