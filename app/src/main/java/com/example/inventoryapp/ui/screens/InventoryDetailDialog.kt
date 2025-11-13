package com.example.inventoryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.InventoryViewModel
import com.example.inventoryapp.model.UserRole
import androidx.compose.runtime.livedata.observeAsState 

@Composable
fun InventoryDetailDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    viewModel: InventoryViewModel,
    userRole: UserRole,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val history by viewModel.transactionHistory.observeAsState(emptyList()) 

    LaunchedEffect(item.serial) { viewModel.loadTransactionHistory(item.serial) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                item.model ?: "Unnamed Item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Item details in cards
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailRow(label = "Serial", value = item.serial)
                        DetailRow(label = "Name", value = item.name)
                        DetailRow(label = "Description", value = item.description.ifBlank { "N/A" })
                    }
                }
                
                // Transaction history
                Text(
                    "Transaction History:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                if (history.isEmpty()) {
                    Text(
                        "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    history.forEach { tx ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "• ${tx.type} on ${tx.date}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (userRole == UserRole.ADMIN) {
                    IconButton(
                        onClick = { onEdit?.invoke() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { 
                                Text(
                                    "Delete Item?",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            },
                            text = { 
                                Text(
                                    "Are you sure you want to delete this item?",
                                    style = MaterialTheme.typography.bodyLarge
                                ) 
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDelete?.invoke()
                                        showDeleteConfirm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { 
                                    Text(
                                        "Delete",
                                        fontWeight = FontWeight.Medium
                                    ) 
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDeleteConfirm = false },
                                    shape = RoundedCornerShape(12.dp)
                                ) { 
                                    Text(
                                        "Cancel",
                                        fontWeight = FontWeight.Medium
                                    ) 
                                }
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Text(
                        "Close",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    ) 
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}