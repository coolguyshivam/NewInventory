package com.example.inventoryapp.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.inventoryapp.model.Transaction

@Composable
fun TransactionDetailDialog(transaction: Transaction, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaction Details") },
        text = { Text("Model: ${transaction.model}\nSerial: ${transaction.serial}\nAmount: ${transaction.amount}\nType: ${transaction.type}") },
        confirmButton = { }
    )
}