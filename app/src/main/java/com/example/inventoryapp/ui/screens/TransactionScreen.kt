package com.example.inventoryapp.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionScreen(
    navController: NavHostController,
    inventoryRepo: InventoryRepository,
    serial: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var model by remember { mutableStateOf("") }
    var modelSuggestions by remember { mutableStateOf(listOf<String>()) }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var quantity by remember { mutableStateOf("1") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            imageUris = uris.take(3)
        }
    }

    // Auto-fetch model from serial (used for sales)
    LaunchedEffect(serial) {
        val item = inventoryRepo.getItemBySerial(serial)
        if (item != null) {
            model = item.model
        }
    }

    // Fetch all model suggestions
    LaunchedEffect(true) {
        modelSuggestions = inventoryRepo.getAllModelNames()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = {}
        ) {
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = aadhaar,
            onValueChange = { aadhaar = it },
            label = { Text("Aadhaar") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(Modifier.height(8.dp))

        // Date picker
        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = "%04d-%02d-%02d".format(y, m + 1, d) },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        .build()
                )
            }
        ) {
            Text("Add Images (Max 3)")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            imageUris.forEach { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                error = null
                if (model.isBlank() || serial.isBlank() || amount.isBlank()) {
                    error = "Model, Serial, and Amount are required"
                    return@Button
                }
                val amountVal = amount.toDoubleOrNull()
                val quantityVal = quantity.toIntOrNull()
                if (amountVal == null || amountVal <= 0) {
                    error = "Amount must be a positive number"
                    return@Button
                }
                if (quantityVal == null || quantityVal <= 0) {
                    error = "Quantity must be a positive integer"
                    return@Button
                }

                loading = true
                scope.launch {
                    val imageUrls = mutableListOf<String>()
                    val storage = FirebaseStorage.getInstance().reference
                    try {
                        for (uri in imageUris) {
                            val ref = storage.child("transactions/${UUID.randomUUID()}.jpg")
                            ref.putFile(uri).await()
                            imageUrls.add(ref.downloadUrl.await().toString())
                        }

                        val transaction = Transaction(
                            type = "Sale",
                            model = model,
                            serial = serial,
                            phone = phone,
                            aadhaar = aadhaar,
                            amount = amountVal,
                            description = description,
                            date = date,
                            quantity = quantityVal,
                            timestamp = System.currentTimeMillis(),
                            imageUrls = imageUrls
                        )

                        val result = inventoryRepo.addTransaction(transaction)
                        loading = false
                        if (result is com.example.inventoryapp.data.Result.Success) {
                            navController.popBackStack()
                        } else if (result is com.example.inventoryapp.data.Result.Error) {
                            error = result.exception.localizedMessage ?: "Failed to save"
                        }
                    } catch (e: Exception) {
                        loading = false
                        error = "Error: ${e.message}"
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Submit")
            }
        }
    }
}
