package com.example.inventoryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.model.InventoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    originalItem: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (InventoryItem, changesSummary: String) -> Unit
) {
    var name by remember { mutableStateOf(originalItem.name) }
    var model by remember { mutableStateOf(originalItem.model) }
    var description by remember { mutableStateOf(originalItem.description) }
    var quantity by remember { mutableStateOf(originalItem.quantity.toString()) }
    
    var contentVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        contentVisible = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Edit Item: ${originalItem.serial}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(400)) + 
                        expandVertically(animationSpec = tween(400))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Create a summary of changes
                    val changes = buildList {
                        if (name != originalItem.name) add("name: '${originalItem.name}' → '$name'")
                        if (model != originalItem.model) add("model: '${originalItem.model}' → '$model'")
                        if (description != originalItem.description) add("description changed")
                        val newQty = quantity.toIntOrNull() ?: originalItem.quantity
                        if (newQty != originalItem.quantity) add("quantity: ${originalItem.quantity} → $newQty")
                    }
                    val changesSummary = if (changes.isEmpty()) "No changes" else changes.joinToString("; ")
                    
                    onSave(
                        originalItem.copy(
                            name = name,
                            model = model,
                            description = description,
                            quantity = quantity.toIntOrNull() ?: originalItem.quantity
                        ),
                        changesSummary
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                ) 
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                ) 
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}