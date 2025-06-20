package com.example.inventoryapp.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var showSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                date = "%02d/%02d/%d".format(d, m + 1, y)
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
            coroutineScope.launch(Dispatchers.IO) {
                val item = inventoryRepo.getItemBySerial(serial)
                if (item != null && item.quantity > 0) {
                    model = item.model
                } else {
                    model = ""
                }
            }
        }
    }

    fun validateAndSubmit() {
        coroutineScope.launch(Dispatchers.IO) {
            val item = inventoryRepo.getItemBySerial(serial)
            if (type == "Sale") {
                if (item == null || item.quantity < 1) {
                    errorMessage = "Cannot sell: item not in inventory or out of stock."
                    showSuccess = false
                    return@launch
                }
            } else if (type == "Purchase") {
                if (item != null) {
                    errorMessage = "Cannot purchase: item with this serial already exists."
                    showSuccess = false
                    return@launch
                }
            }
            // Validation for inputs
            if (serial.isBlank() || model.isBlank() || amount.isBlank() || date.isBlank()) {
                errorMessage = "Please fill all fields."
                showSuccess = false
                return@launch
            }
            val parsedAmount = amount.toDoubleOrNull()
            if (parsedAmount == null) {
                errorMessage = "Amount must be a valid number."
                showSuccess = false
                return@launch
            }
            val tx = Transaction(
                serial = serial,
                model = model,
                amount = parsedAmount,
                description = description,
                date = date,
                type = type,
                quantity = 1
            )
            inventoryRepo.addTransaction(tx)
            errorMessage = null
            showSuccess = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFB3CFF2), Color(0xFFFDEB71))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Modern heading
            Text(
                if (type == "Purchase") "New Purchase" else "New Sale",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Type Toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SegmentedButton(
                    options = listOf("Purchase", "Sale"),
                    selected = type,
                    onSelected = { type = it }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Serial and barcode
            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                label = { Text("Serial Number") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { navController.navigate("barcode_scanner") }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Model
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = type == "Purchase",
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                shape = RoundedCornerShape(16.dp),
                maxLines = 3
            )

            Spacer(Modifier.height(12.dp))

            // Date
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (date.isBlank()) "Pick Date" else date)
            }

            Spacer(Modifier.height(18.dp))

            // Submit button
            Button(
                onClick = { validateAndSubmit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Submit Transaction", style = MaterialTheme.typography.titleMedium)
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                errorMessage?.let {
                    Text(
                        it,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    "Transaction successful!",
                    color = Color(0xFF388E3C),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SegmentedButton(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        Modifier
            .background(
                color = Color(0xFFF0F0F0),
                shape = CircleShape
            )
            .padding(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
            TextButton(
                onClick = { onSelected(option) },
                shape = CircleShape,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = bgColor,
                    contentColor = textColor
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
            ) {
                Text(option)
            }
        }
    }
}