package com.example.inventoryapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

// Move this to model/Transaction.kt if used elsewhere!
data class Transaction(
    val type: String,
    val model: String,
    val serial: String,
    val phone: String?,
    val aadhaar: String?,
    val amount: Double,
    val date: String,
    val quantity: Int,
    val description: String?
)

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
            Text("Type: ${transaction.type}")
            Text("Model: ${transaction.model}")
            Text("Serial: ${transaction.serial}")
            transaction.phone?.let { Text("Phone: $it") }
            transaction.aadhaar?.let { Text("Aadhaar: $it") }
            Text("Amount: \u20b9${transaction.amount}")
            Text("Quantity: ${transaction.quantity}")
            Text("Date: ${transaction.date}")
            transaction.description?.takeIf { it.isNotBlank() }?.let {
                Text("Description: $it")
            }
        }
    }
}