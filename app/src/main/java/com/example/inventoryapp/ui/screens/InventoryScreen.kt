package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.Result
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(
    navController: NavHostController,
    inventoryRepo: InventoryRepository,
    authRepo: AuthRepository
) {
    var items by remember { mutableStateOf(emptyList<com.example.inventoryapp.model.InventoryItem>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        when (val result = inventoryRepo.getInventory()) {
            is Result.Success -> items = result.data
            is Result.Error -> error = result.exception.message
            else -> {}
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { navController.navigate("addEditItem") }) { Text("+ Add Item") }
            Button(onClick = {
                authRepo.logout()
                navController.navigate("login") { popUpTo("inventory") { inclusive = true } }
            }) { Text("Logout") }
        }
        Spacer(Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        } else {
            items.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = {
                    navController.navigate("transaction/${item.serial}")
                }) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Model: ${item.model}")
                        Text("Serial: ${item.serial}")
                        Text("Date: ${item.date}")
                    }
                }
            }
        }
    }
}