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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    serial: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
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
    var loading by remember { mutableStateOf(false) }

    // Field errors
    var serialError by remember { mutableStateOf<String?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    // Focus Requesters
    val (serialFocus, modelFocus, phoneFocus, aadhaarFocus, amountFocus, descriptionFocus, quantityFocus) = List(7) { remember { FocusRequester() } }

    // For date picker validation (no future dates)
    fun isDateValid(selected: Calendar): Boolean {
        val now = Calendar.getInstance()
        return !selected.after(now)
    }

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

    // Phone number formatting: allow only digits and max 10 chars
    fun formatPhone(input: String): String = input.filter { it.isDigit() }.take(10)

    // Aadhaar formatting: allow only digits and max 12 chars
    fun formatAadhaar(input: String): String = input.filter { it.isDigit() }.take(12)

    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        images = uris?.take(5) ?: emptyList()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = serialState,
                onValueChange = {
                    serialState = it
                    isModelAuto = false
                    serialError = null
                },
                label = { Text("Serial") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(serialFocus),
                singleLine = true,
                isError = serialError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onNext = { modelFocus.requestFocus() }
                ),
                enabled = !loading
            )
            if (serialError != null) {
                Text(serialError!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("barcode_scanner") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text("Scan Barcode")
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = model,
                onValueChange = {
                    if (!isModelAuto) model = it
                    modelError = null
                },
                label = { Text("Model") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(modelFocus),
                singleLine = true,
                enabled = !isModelAuto && !loading,
                isError = modelError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onNext = { phoneFocus.requestFocus() }
                )
            )
            if (modelError != null) {
                Text(modelError!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = formatPhone(it) },
                label = { Text("Phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneFocus),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onNext = { aadhaarFocus.requestFocus() }
                ),
                enabled = !loading
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = aadhaar,
                onValueChange = { aadhaar = formatAadhaar(it) },
                label = { Text("Aadhaar") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(aadhaarFocus),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onNext = { amountFocus.requestFocus() }
                ),
                enabled = !loading
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it.filter { ch -> ch.isDigit() }
                    amountError = null
                },
                label = { Text("Amount") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocus),
                singleLine = true,
                isError = amountError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onNext = { descriptionFocus.requestFocus() }
                ),
                enabled = !loading
            )
            if (amountError != null) {
                Text(amountError!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onNext = { quantityFocus.requestFocus() }
                ),
                enabled = !loading
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { /* Date picker only */ },
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !loading) {
                        val calendar = Calendar.getInstance()
                        val datePicker = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val picked = Calendar.getInstance()
                                picked.set(year, month, dayOfMonth)
                                if (isDateValid(picked)) {
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(picked.time)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Future dates are not allowed.")
                                    }
                                }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePicker.datePicker.maxDate = System.currentTimeMillis()
                        datePicker.show()
                    },
                readOnly = true,
                enabled = !loading
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = quantity,
                onValueChange = {
                    quantity = it.filter { ch -> ch.isDigit() }
                    quantityError = null
                },
                label = { Text("Quantity") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(quantityFocus),
                singleLine = true,
                isError = quantityError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                enabled = !loading
            )
            if (quantityError != null) {
                Text(quantityError!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { imgPicker.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text("Pick Images (max 5)")
            }

            if (images.isNotEmpty()) {
                Column {
                    Text("Tap an image to remove")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        images.forEach { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(model = uri),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable(enabled = !loading) {
                                        images = images - uri
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    // Field validation
                    var valid = true
                    serialError = null
                    modelError = null
                    amountError = null
                    quantityError = null

                    if (serialState.isBlank()) {
                        serialError = "Serial is required"
                        valid = false
                    }
                    if (model.isBlank()) {
                        modelError = "Model is required"
                        valid = false
                    }
                    val amountInt = amount.toIntOrNull()
                    if (amount.isBlank()) {
                        amountError = "Amount is required"
                        valid = false
                    } else if (amountInt == null || amountInt <= 0) {
                        amountError = "Enter a valid positive number"
                        valid = false
                    }
                    val quantityInt = quantity.toIntOrNull()
                    if (quantity.isBlank()) {
                        quantityError = "Quantity is required"
                        valid = false
                    } else if (quantityInt == null || quantityInt <= 0) {
                        quantityError = "Enter a valid positive number"
                        valid = false
                    }

                    if (!valid) return@Button

                    loading = true

                    scope.launch {
                        try {
                            // --- Image upload ---
                            val imageUrls = mutableListOf<String>()
                            if (images.isNotEmpty()) {
                                val storage = FirebaseStorage.getInstance().reference
                                for ((index, uri) in images.withIndex()) {
                                    val ref = storage.child("transactions/${serialState}_${System.currentTimeMillis()}_$index.jpg")
                                    ref.putFile(uri).await()
                                    imageUrls += ref.downloadUrl.await().toString()
                                }
                            }
                            // --- Save transaction ---
                            val transaction = Transaction(
                                serial = serialState,
                                model = model,
                                phone = phone,
                                aadhaar = aadhaar,
                                amount = amountInt ?: 0,
                                description = description,
                                date = date,
                                quantity = quantityInt ?: 1,
                                imageUrls = imageUrls // Will be empty if no images
                            )
                            val result = inventoryRepo.addTransaction(transaction)
                            loading = false
                            if (result is Result.Success) {
                                snackbarHostState.showSnackbar("Transaction saved successfully!")
                                // Optionally clear the form on success:
                                serialState = ""
                                model = ""
                                isModelAuto = false
                                phone = ""
                                aadhaar = ""
                                amount = ""
                                description = ""
                                quantity = "1"
                                images = emptyList()
                                // Or navigate back:
                                // navController.popBackStack()
                            } else if (result is Result.Error) {
                                snackbarHostState.showSnackbar(result.exception?.message ?: "Error saving transaction.")
                            }
                        } catch (e: Exception) {
                            loading = false
                            snackbarHostState.showSnackbar(e.message ?: "Unknown error occurred")
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
}