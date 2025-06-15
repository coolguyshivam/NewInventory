package com.example.inventoryapp.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
        images = uris?.take(3) ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            onClick = { navController.navigate("barcode_scan") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Barcode")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it; isModelAuto = false },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = isModelAuto,
            enabled = !isModelAuto,
            trailingIcon = { if (isModelAuto) Icon(Icons.Default.Lock, contentDescription = "Auto-filled") }
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
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = date,
            onValueChange = {}, // Date is only changed via the picker
            label = { Text("Date") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = "%04d-%02d-%02d".format(y, m + 1, d) },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
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
        Button(onClick = {
            imgPicker.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }) {
            Text("Add Images (Max 3)")
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            images.forEach { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        if (error != null) Text(text = error!!, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                error = null
                val amt = amount.toDoubleOrNull()
                val qty = quantity.toIntOrNull()
                if (serialState.isBlank() || model.isBlank() || amt == null || amt <= 0 || qty == null || qty <= 0) {
                    error = "Check Serial, Model, positive Amount & Quantity"
                    return@Button
                }
                loading = true
                scope.launch {
                    try {
                        val stRef = FirebaseStorage.getInstance().reference
                        val urls = mutableListOf<String>()
                        images.forEach { u ->
                            val ref = stRef.child("transactions/${UUID.randomUUID()}.jpg")
                            ref.putFile(u).await()
                            urls += ref.downloadUrl.await().toString()
                        }
                        val txn = Transaction(
                            type = "Sale", model = model, serial = serialState,
                            phone = phone, aadhaar = aadhaar,
                            amount = amt, description = description, date = date,
                            quantity = qty, timestamp = System.currentTimeMillis(), imageUrls = urls
                        )
                        when (val res = inventoryRepo.addTransaction(txn)) {
                            is Result.Success -> navController.popBackStack()
                            is Result.Error -> error = res.exception.localizedMessage
                        }
                    } catch (e: Exception) {
                        error = e.localizedMessage
                    } finally { loading = false }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Submit")
        }
    }
}