package com.example.inventoryapp.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.Transaction
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
) {
    // UI State
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val savedState = navController.currentBackStackEntry?.savedStateHandle

    // Form State
    var type by remember { mutableStateOf(savedState?.get<String>("type") ?: "Purchase") }
    var serialState by remember { mutableStateOf(savedState?.get<String>("serial") ?: "") }
    var model by remember { mutableStateOf(savedState?.get<String>("model") ?: "") }
    var isModelAuto by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var quantity by remember { mutableStateOf("1") }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    // Field errors
    var serialError by remember { mutableStateOf<String?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    // Focus Requesters
    val serialFocus = remember { FocusRequester() }
    val modelFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val aadhaarFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }
    val quantityFocus = remember { FocusRequester() }

    // Utils
    fun formatPhone(input: String) = input.filter { it.isDigit() }.take(10)
    fun formatAadhaar(input: String) = input.filter { it.isDigit() }.take(12)
    fun isDateValid(selected: Calendar): Boolean = !selected.after(Calendar.getInstance())

    // Image Picker
    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        images = uris?.take(5) ?: emptyList()
    }

    // Handle scanned serial from BarcodeScanner
    LaunchedEffect(savedState?.get<String>("scannedSerial")) {
        savedState?.get<String>("scannedSerial")?.let { code ->
            serialState = code
            savedState.remove<String>("scannedSerial")
        }
    }

    // Auto-fetch model when serial or type changes
    LaunchedEffect(serialState, type) {
        if (serialState.isNotBlank() && type == "Sale") {
            scope.launch(Dispatchers.IO) {
                val item = inventoryRepo.getItemBySerial(serialState)
                if (item != null && item.quantity > 0) {
                    model = item.model
                    isModelAuto = true
                } else {
                    model = ""
                    isModelAuto = false
                }
            }
        } else if (type == "Purchase") {
            isModelAuto = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

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
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(24.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Heading
                Text(
                    if (type == "Purchase") "New Purchase" else "New Sale",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Type Toggle
                SegmentedButton(
                    options = listOf("Purchase", "Sale"),
                    selected = type,
                    onSelected = {
                        type = it
                        // Clear model if switching to purchase
                        if (type == "Purchase") {
                            model = ""
                            isModelAuto = false
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Serial Number
                OutlinedTextField(
                    value = serialState,
                    onValueChange = {
                        serialState = it
                        serialError = null
                        isModelAuto = false
                    },
                    label = { Text("Serial Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(serialFocus),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { navController.navigate("barcode_scanner") }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    },
                    isError = serialError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { modelFocus.requestFocus() }
                    ),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )
                serialError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(8.dp))

                // Model
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
                    enabled = !isModelAuto && type == "Purchase" && !loading,
                    isError = modelError != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { phoneFocus.requestFocus() }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                modelError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(8.dp))

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = formatPhone(it) },
                    label = { Text("Phone (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(phoneFocus),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { aadhaarFocus.requestFocus() }
                    ),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Aadhaar
                OutlinedTextField(
                    value = aadhaar,
                    onValueChange = { aadhaar = formatAadhaar(it) },
                    label = { Text("Aadhaar (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(aadhaarFocus),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { amountFocus.requestFocus() }
                    ),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        val filtered = it.filterIndexed { idx, ch -> ch.isDigit() || (ch == '.' && !it.take(idx).contains('.')) }
                        amount = filtered
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
                    keyboardActions = KeyboardActions(
                        onNext = { descriptionFocus.requestFocus() }
                    ),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )
                amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(descriptionFocus),
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { quantityFocus.requestFocus() }
                    ),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Date
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
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
                        ).apply {
                            datePicker.maxDate = System.currentTimeMillis()
                        }.show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (date.isBlank()) "Pick Date" else date)
                }

                Spacer(Modifier.height(8.dp))

                // Quantity
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
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp)
                )
                quantityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Spacer(Modifier.height(8.dp))

                // Image Picker
                Button(
                    onClick = { imgPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = images.size < 5 && !loading
                ) {
                    Text("Pick Images (max 5)")
                }

                if (images.isNotEmpty()) {
                    Column {
                        Text("Tap an image to remove")
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            images.forEach { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(model = uri),
                                    contentDescription = "Selected image",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !loading) {
                                            images = images - uri
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        // Reset errors
                        serialError = null
                        modelError = null
                        amountError = null
                        quantityError = null

                        var valid = true

                        if (serialState.isBlank()) {
                            serialError = "Serial is required"
                            valid = false
                        }
                        if (model.isBlank()) {
                            modelError = "Model is required"
                            valid = false
                        }
                        val amountDouble = amount.toDoubleOrNull()
                        if (amount.isBlank()) {
                            amountError = "Amount is required"
                            valid = false
                        } else if (amountDouble == null || amountDouble <= 0.0) {
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
                                // --- Image upload (if any) ---
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
                                    amount = amountDouble ?: 0.0,
                                    description = description,
                                    date = date,
                                    quantity = quantityInt ?: 1,
                                    imageUrls = imageUrls,
                                    type = type
                                )

                                // Logic for Sale/Purchase
                                val item = inventoryRepo.getItemBySerial(serialState)
                                if (type == "Sale") {
                                    if (item == null || item.quantity < 1) {
                                        snackbarHostState.showSnackbar("Cannot sell: item not in inventory or out of stock.")
                                        loading = false
                                        return@launch
                                    }
                                } else if (type == "Purchase") {
                                    if (item != null) {
                                        snackbarHostState.showSnackbar("Cannot purchase: item with this serial already exists.")
                                        loading = false
                                        return@launch
                                    }
                                }

                                val result = inventoryRepo.addTransaction(transaction)
                                loading = false
                                if (result is Result.Success) {
                                    showSuccess = true
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
                                    // navController.popBackStack() // OR stay on the page
                                } else if (result is Result.Error) {
                                    snackbarHostState.showSnackbar(result.exception?.message ?: "Error saving transaction.")
                                }
                            } catch (e: Exception) {
                                loading = false
                                snackbarHostState.showSnackbar(e.message ?: "Unknown error occurred")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Save Transaction", style = MaterialTheme.typography.titleMedium)
                }

                // Animated error/success feedback
                AnimatedVisibility(
                    visible = serialError != null || modelError != null || amountError != null || quantityError != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column {
                        serialError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        }
                        modelError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        }
                        amountError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        }
                        quantityError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        }
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