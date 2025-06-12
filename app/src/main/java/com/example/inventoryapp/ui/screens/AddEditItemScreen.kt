package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddEditItemScreen(
    navController: NavHostController,
    inventoryRepo: InventoryRepository,
    itemId: String? = null
) {
    var model by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = serial, onValueChange = { serial = it }, label = { Text("Serial") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = aadhaar, onValueChange = { aadhaar = it }, label = { Text("Aadhaar") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                if (model.isBlank() || serial.isBlank()) {
                    error = "Model and Serial are required"
                    return@Button
                }
                scope.launch {
                    val item = InventoryItem(
                        model = model,
                        serial = serial,
                        phone = phone,
                        aadhaar = aadhaar,
                        description = description,
                        date = date,
                        timestamp = System.currentTimeMillis()
                    )
                    when (val res = inventoryRepo.addInventoryItem(item)) {
                        is com.example.inventoryapp.data.Result.Success -> navController.popBackStack()
                        is com.example.inventoryapp.data.Result.Error -> error = res.exception.message
                        else -> {}
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save")
        }
    }
}