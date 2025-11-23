package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
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
    var purchaseDate by remember { mutableStateOf(originalItem.date) }
    var customerName by remember { mutableStateOf(originalItem.customerName) }
    var mobileNumber by remember { mutableStateOf(originalItem.phone) }
    var adharNumber by remember { mutableStateOf(originalItem.aadhaar) }
    var purchasePrice by remember { mutableStateOf(originalItem.purchasePrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item: ${originalItem.serial}") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("Purchase Date (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adharNumber,
                    onValueChange = { adharNumber = it },
                    label = { Text("Aadhaar Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = { Text("Purchase Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
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
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default  // Allow Enter key for newlines
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                // Create a summary of changes (excluding timestamp)
                val changes = buildList {
                    if (name != originalItem.name) add("name: '${originalItem.name}' → '$name'")
                    if (model != originalItem.model) add("model: '${originalItem.model}' → '$model'")
                    if (description != originalItem.description) add("description changed")
                    val newQty = quantity.toIntOrNull() ?: originalItem.quantity
                    if (newQty != originalItem.quantity) add("quantity: ${originalItem.quantity} → $newQty")
                    if (purchaseDate != originalItem.date) add("purchase date: '${originalItem.date}' → '$purchaseDate'")
                    if (customerName != originalItem.customerName) add("customer name: '${originalItem.customerName}' → '$customerName'")
                    if (mobileNumber != originalItem.phone) add("mobile: '${originalItem.phone}' → '$mobileNumber'")
                    if (adharNumber != originalItem.aadhaar) add("aadhaar: '${originalItem.aadhaar}' → '$adharNumber'")
                    val newPrice = purchasePrice.toDoubleOrNull() ?: originalItem.purchasePrice
                    if (newPrice != originalItem.purchasePrice) add("purchase price: ${originalItem.purchasePrice} → $newPrice")
                }
                val changesSummary = if (changes.isEmpty()) "No changes" else changes.joinToString("; ")
                
                // Only save if there are changes (excluding timestamp)
                if (changes.isNotEmpty()) {
                    onSave(
                        originalItem.copy(
                            name = name,
                            model = model,
                            description = description,
                            quantity = quantity.toIntOrNull() ?: originalItem.quantity,
                            date = purchaseDate,
                            customerName = customerName,
                            phone = mobileNumber,
                            aadhaar = adharNumber,
                            purchasePrice = purchasePrice.toDoubleOrNull() ?: originalItem.purchasePrice
                        ),
                        changesSummary
                    )
                } else {
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}