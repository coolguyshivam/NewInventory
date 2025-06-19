package com.example.inventoryapp.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository
) {
    val navBackStackEntry = navController.currentBackStackEntry
    val initialType = navBackStackEntry?.arguments?.getString("type") ?: "Purchase"
    val initialSerial = navBackStackEntry?.arguments?.getString("serial") ?: ""
    val initialModel = navBackStackEntry?.arguments?.getString("model") ?: ""

    var type by remember { mutableStateOf(initialType) }
    var serial by remember { mutableStateOf(initialSerial) }
    var model by remember { mutableStateOf(initialModel) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Barcode scan integration
    val scannedSerial = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("scannedSerial")
    LaunchedEffect(scannedSerial) {
        if (!scannedSerial.isNullOrBlank()) {
            serial = scannedSerial
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scannedSerial")
        }
    }

    // Date picker
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                date = "${d}/${m + 1}/$y"
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Auto-fill model for Sale
    LaunchedEffect(serial, type) {
        if (type == "Sale" && serial.isNotBlank()) {
            val item = inventoryRepo.getItemBySerial(serial)
            if (item != null && item.quantity > 0) {
                model = item.model
            } else {
                model = ""
            }
        }
    }

    fun validateAndSubmit() {
        val item = inventoryRepo.getItemBySerial(serial)
        if (type == "Sale") {
            if (item == null || item.quantity < 1) {
                errorMessage = "Cannot sell: item not in inventory or out of stock."
                return
            }
        } else if (type == "Purchase") {
            if (item != null) {
                errorMessage = "Cannot purchase: item with this serial already exists."
                return
            }
        }
        val tx = Transaction(
            serial = serial,
            model = model,
            amount = amount.toDoubleOrNull() ?: 0.0,
            description = description,
            date = date,
            type = type,
            quantity = 1
        )
        inventoryRepo.addTransaction(tx)
        errorMessage = null
        // Optionally: navigate back or clear fields
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Type:")
            Spacer(Modifier.width(8.dp))
            DropdownMenuBox(
                items = listOf("Purchase", "Sale"),
                selected = type,
                onSelectedChange = { type = it }
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                label = { Text("Serial Number") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { navController.navigate("barcode_scanner") }) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = type == "Purchase"
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (date.isBlank()) "Pick Date" else date)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { validateAndSubmit() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Transaction")
        }
        errorMessage?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun DropdownMenuBox(
    items: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true }
        ) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelectedChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}