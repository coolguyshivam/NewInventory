package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionScreen(
    navController: NavHostController,
    inventoryRepo: InventoryRepository,
    serial: String
) {
    var model by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var quantity by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = aadhaar, onValueChange = { aadhaar = it }, label = { Text("Aadhaar") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            if (model.isBlank() || serial.isBlank() || amount.isBlank()) {
                error = "Model, Serial and Amount are required"
                return@Button
            }
            scope.launch {
                val trans = Transaction(
                    type = "Sale",
                    model = model,
                    serial = serial,
                    phone = phone,
                    aadhaar = aadhaar,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    description = description,
                    date = date,
                    quantity = quantity.toIntOrNull() ?: 1,
                    timestamp = System.currentTimeMillis()
                )
                when (val res = inventoryRepo.addTransaction(trans)) {
                    is com.example.inventoryapp.data.Result.Success -> navController.popBackStack()
                    is com.example.inventoryapp.data.Result.Error -> error = res.exception.message
                    else -> {}
                }
            }
        }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Submit")
        }
    }
}