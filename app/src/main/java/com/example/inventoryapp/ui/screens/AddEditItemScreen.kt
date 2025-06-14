package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.data.Result
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun AddEditItemScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    itemId: String?
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                loading = true
                error = null
                // In a real app, launch a coroutine or use ViewModel
            },
            enabled = !loading
        ) {
            Text("Save")
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (loading) CircularProgressIndicator()
    }
}