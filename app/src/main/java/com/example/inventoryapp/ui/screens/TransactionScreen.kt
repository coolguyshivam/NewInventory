package com.example.inventoryapp.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    serial: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedState = navController.currentBackStackEntry?.savedStateHandle

    var serialState by remember { mutableStateOf(serial) }
    var model by remember { mutableStateOf("") }
    var isModelAuto by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var quantity by remember { mutableStateOf("1") }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    // Handle scanned serial from BarcodeScanner
    LaunchedEffect(savedState?.get<String>("scannedSerial")) {
        savedState?.get<String>("scannedSerial")?.let { code ->
            serialState = code
            savedState.remove<String>("scannedSerial")
        }
    }

    // Auto-fetch model when serial changes
    LaunchedEffect(serialState) {
        if (serialState.isNotBlank()) {
            val item = inventoryRepo.getItemBySerial(serialState)
            if (item != null) {
                model = item.model
                isModelAuto = true
            } else {
                model = ""
                isModelAuto = false
            }
        } else {
            model = ""
            isModelAuto = false
        }
    }

    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        images = uris?.take(5) ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = serialState,
            onValueChange = {
                serialState = it
                isModelAuto = false
            },
            label = { Text("Serial") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate("barcode_scanner") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Barcode")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { if (!isModelAuto) model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isModelAuto
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = aadhaar,
            onValueChange = { aadhaar = it },
            label = { Text("Aadhaar") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val calendar = Calendar.getInstance()
                    val datePicker = DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val picked = Calendar.getInstance()
                            picked.set(year, month, dayOfMonth)
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.time)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    datePicker.show()
                },
            readOnly = true
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { imgPicker.launch(ActivityResultContracts.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pick Images (max 5)")
        }

        if (images.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                images.forEach { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                loading = true
                error = null

                // Simple validation
                if (serialState.isBlank() || model.isBlank() || amount.isBlank() || quantity.isBlank()) {
                    error = "Please fill all required fields."
                    loading = false
                    return@Button
                }

                val amountInt = amount.toIntOrNull()
                val quantityInt = quantity.toIntOrNull()
                if (amountInt == null || quantityInt == null) {
                    error = "Amount and Quantity must be numbers."
                    loading = false
                    return@Button
                }

                scope.launch {
                    try {
                        // --- Image upload ---
                        val imageUrls = mutableListOf<String>()
                        if (images.isNotEmpty()) {
                            for ((index, uri) in images.withIndex()) {
                                val fileName = "${serialState}_${System.currentTimeMillis()}_$index.jpg"
                                when (val uploadResult = inventoryRepo.uploadImage(uri, fileName)) {
                                    is Result.Success -> imageUrls.add(uploadResult.data)
                                    is Result.Error -> {
                                        error = "Image upload failed: ${uploadResult.exception?.message ?: ""}"
                                        loading = false
                                        return@launch
                                    }
                                }
                            }
                        }
                        // --- Save transaction ---
                        val transaction = Transaction(
                            serial = serialState,
                            model = model,
                            phone = phone,
                            aadhaar = aadhaar,
                            amount = amountInt,
                            description = description,
                            date = date,
                            quantity = quantityInt,
                            imageUrls = imageUrls // Will be empty if no images
                        )
                        val result = inventoryRepo.addTransaction(transaction)
                        loading = false
                        if (result is Result.Success) {
                            navController.popBackStack()
                        } else if (result is Result.Error) {
                            error = result.exception?.message ?: "Error saving transaction."
                        }
                    } catch (e: Exception) {
                        loading = false
                        error = e.message ?: "Unknown error occurred"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Save Transaction")
        }
    }
}