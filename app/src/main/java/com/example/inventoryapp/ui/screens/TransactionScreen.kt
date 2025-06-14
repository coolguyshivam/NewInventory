package com.example.inventoryapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

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
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
        if (uris != null) {
            imageUris = uris.take(3)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
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
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
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
        Button(
            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Add Images (Max 3)")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(error ?: "", color = MaterialTheme.colorScheme.error)
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
                    // Upload images to Firebase Storage and collect URLs
                    val imageUrls = mutableListOf<String>()
                    val storage = FirebaseStorage.getInstance().reference
                    imageUris.forEachIndexed { idx, uri ->
                        val ref = storage.child("transactions/${UUID.randomUUID()}.jpg")
                        val uploadTask = ref.putFile(uri)
                        uploadTask.await()
                        val url = ref.downloadUrl.await().toString()
                        imageUrls.add(url)
                    }
                    val trans = Transaction(
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
                    when (val res = inventoryRepo.addTransaction(trans)) {
                        is com.example.inventoryapp.data.Result.Success -> {
                            loading = false
                            navController.popBackStack()
                        }
                        is com.example.inventoryapp.data.Result.Error -> {
                            loading = false
                            error = res.exception.localizedMessage ?: "Unknown error"
                        }
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp),
            enabled = !loading
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