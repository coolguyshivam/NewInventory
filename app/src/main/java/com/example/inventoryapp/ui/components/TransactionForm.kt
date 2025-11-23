package com.example.inventoryapp.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.ItemStatus
import com.example.inventoryapp.model.Transaction
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.utils.ImageUtils
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    userRole: UserRole,
    requiredFields: List<String>,
    snackbarHostState: SnackbarHostState,
    showSuccess: MutableState<Boolean>,
    prefillType: String? = null,
    prefillSerial: String? = null,
    prefillModel: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val transactionTypes = listOf("Purchase", "Sale", "Repair", "Repair Return")
    var type by remember { mutableStateOf(prefillType ?: transactionTypes.first()) }
    var serial by remember { mutableStateOf(prefillSerial ?: "") }
    var model by remember { mutableStateOf(prefillModel ?: "") }
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }

    var serialError by remember { mutableStateOf<String?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var customerNameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var imageLimitError by remember { mutableStateOf<String?>(null) }

    val serialFocus = remember { FocusRequester() }
    val modelFocus = remember { FocusRequester() }
    val customerNameFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }
    val aadhaarFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val descriptionFocus = remember { FocusRequester() }
    val quantityFocus = remember { FocusRequester() }

    // Model suggestions
    var modelSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(model) {
        if (model.isNotBlank()) {
            val models = inventoryRepo.getAllModels()
            modelSuggestions = models.filter { it.contains(model, ignoreCase = true) }.take(4)
        } else {
            modelSuggestions = emptyList()
        }
    }

    // Permissions state/messages
    var galleryDeniedReason by remember { mutableStateOf<String?>(null) }
    var cameraDeniedReason by remember { mutableStateOf<String?>(null) }

    val maxImages = 10
    var imageSourceSheetOpen by remember { mutableStateOf(false) }

    val imagePickerHandler = remember(context) {
        ImagePickerHandler(
            context = context,
            maxImages = maxImages,
            onGalleryDenied = { galleryDeniedReason = it },
            onCameraDenied = { cameraDeniedReason = it },
            onImagesSelected = { uris ->
                if (images.size + uris.size > maxImages) {
                    imageLimitError = "You can select up to $maxImages images per transaction."
                } else {
                    images = images + uris.take(maxImages - images.size)
                    imageLimitError = null
                }
            },
            onImageCaptured = { uri ->
                if (images.size < maxImages) {
                    images = images + uri
                    imageLimitError = null
                } else {
                    imageLimitError = "You can select up to $maxImages images per transaction."
                }
            }
        )
    }

    // Setup gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (images.size + uris.size > maxImages) {
                imageLimitError = "You can select up to $maxImages images per transaction."
            } else {
                images = images + uris.take(maxImages - images.size)
                imageLimitError = null
            }
        }
    }

    // Setup camera launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            if (images.size < maxImages) {
                images = images + cameraImageUri!!
                imageLimitError = null
            } else {
                imageLimitError = "You can select up to $maxImages images per transaction."
            }
        }
    }

    // Setup camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerHandler.launchCamera()
        } else {
            cameraDeniedReason = "Camera permission is required to take photos. Please enable it in settings."
        }
    }

    // Update imagePickerHandler to use cameraImageUri
    LaunchedEffect(Unit) {
        imagePickerHandler.setupLaunchers(galleryLauncher, cameraLauncher) { uri ->
            cameraImageUri = uri
        }
    }

    if (galleryDeniedReason != null) {
        LaunchedEffect(galleryDeniedReason) {
            snackbarHostState.showSnackbar(galleryDeniedReason!!)
            galleryDeniedReason = null
        }
    }
    if (cameraDeniedReason != null) {
        LaunchedEffect(cameraDeniedReason) {
            snackbarHostState.showSnackbar(cameraDeniedReason!!)
            cameraDeniedReason = null
        }
    }
    if (imageLimitError != null) {
        LaunchedEffect(imageLimitError) {
            snackbarHostState.showSnackbar(imageLimitError!!)
            imageLimitError = null
        }
    }

    LaunchedEffect(serial, type) {
        if (serial.isNotBlank() && type != "Purchase") {
            coroutineScope.launch(Dispatchers.IO) {
                val item = inventoryRepo.getItemBySerial(serial)
                if (item != null && item.quantity > 0) {
                    model = item.model
                }
            }
        }
    }

    // Handle barcode scanner result
    val scannedSerialLive = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("scannedSerial")
    val scannedSerialState = scannedSerialLive?.observeAsState()
    val scannedSerialResult = scannedSerialState?.value
    LaunchedEffect(scannedSerialResult) {
        scannedSerialResult?.let { scannedSerial ->
            serial = scannedSerial
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scannedSerial")
        }
    }

    // Date picker dialog
    var datePickerDialogOpen by remember { mutableStateOf(false) }
    if (datePickerDialogOpen) {
        val calendar = Calendar.getInstance()
        val parts = date.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(y, m, d)
                if (!selectedCal.after(Calendar.getInstance())) {
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                } else {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Future dates are not allowed.") }
                }
                datePickerDialogOpen = false
            },
            year, month, day
        ).apply {
            datePicker.maxDate = calendar.timeInMillis
        }.show()
    }

    fun formatPhone(input: String) = input.filter { it.isDigit() }.take(10)
    fun formatAadhaar(input: String) = input.filter { it.isDigit() }.take(12)
    val canEdit = userRole == UserRole.ADMIN || userRole == UserRole.STAFF

    // Prevent navigation while uploading
    if (uploading) {
        AlertDialog(
            onDismissRequest = { /* Block dismiss */ },
            confirmButton = {},
            title = { Text("Please Wait") },
            text = { Text("Data Uploading") }
        )
    }

    Box(
        modifier = modifier
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
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "New Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                transactionTypes.forEach { t ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clickable { type = t },
                        colors = CardDefaults.cardColors(
                            containerColor = if (type == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t,
                                color = if (type == t) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = serial,
                onValueChange = {
                    serial = it
                    serialError = null
                },
                label = { Text("Serial Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(serialFocus),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { navController.navigate("barcode_reader") },
                        modifier = Modifier.testTag("barcodeIcon")
                    ) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                isError = serialError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { modelFocus.requestFocus() }
                ),
                enabled = canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp)
            )
            serialError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize) }

            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    modelError = null
                },
                label = { Text("Model") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(modelFocus),
                singleLine = true,
                enabled = canEdit && !loading && !uploading,
                isError = modelError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { customerNameFocus.requestFocus() }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            modelError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize) }

            AnimatedVisibility(visible = modelSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F8))
                ) {
                    Column {
                        modelSuggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        model = suggestion
                                        modelSuggestions = emptyList()
                                    }
                                    .padding(10.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    customerName = it
                    customerNameError = null
                },
                label = { Text("Customer Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(customerNameFocus),
                singleLine = true,
                enabled = canEdit && !loading && !uploading,
                isError = customerNameError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { phoneFocus.requestFocus() }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            customerNameError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize) }

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
                enabled = canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp)
            )

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
                enabled = canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp)
            )

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
                enabled = canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp)
            )
            amountError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = MaterialTheme.typography.bodySmall.fontSize) }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .focusRequester(descriptionFocus),
                singleLine = false,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default  // Allow Enter key for newlines
                ),
                enabled = canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedButton(
                onClick = { datePickerDialogOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                enabled = canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFEAF1FB)
                )
            ) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (date.isBlank()) "Pick Date" else date, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = "1",
                onValueChange = {},
                label = { Text("Quantity") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(quantityFocus),
                singleLine = true,
                isError = false,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                enabled = false,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { imageSourceSheetOpen = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = images.size < maxImages && canEdit && !loading && !uploading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFAF8F4)
                )
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Add Images", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("${images.size}/$maxImages", color = Color.Gray)
            }

            if (imageSourceSheetOpen) {
                ModalBottomSheet(
                    onDismissRequest = { imageSourceSheetOpen = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Take Photo") },
                        leadingContent = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            imageSourceSheetOpen = false
                            // Request camera permission before launching camera
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            imageSourceSheetOpen = false
                            imagePickerHandler.launchGallery()
                        }
                    )
                }
            }

            if (images.isNotEmpty()) {
                Column(Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                    Text("Tap an image to remove", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        images.forEach { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(model = uri),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F3F3))
                                    .clickable(enabled = canEdit && !loading && !uploading) {
                                        images = images - uri
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (loading || uploading) return@Button  // Prevent multiple clicks
                    
                    serialError = null
                    modelError = null
                    amountError = null

                    var valid = true
                    if ("serial" in requiredFields && serial.isBlank()) {
                        serialError = "Serial is required"
                        valid = false
                    }
                    if ("model" in requiredFields && model.isBlank()) {
                        modelError = "Model is required"
                        valid = false
                    }
                    val amountDouble = amount.toDoubleOrNull()
                    if ("amount" in requiredFields && amount.isBlank()) {
                        amountError = "Amount is required"
                        valid = false
                    } else if (amountDouble == null || amountDouble <= 0.0) {
                        amountError = "Enter a valid positive number"
                        valid = false
                    }
                    if ("date" in requiredFields && date.isBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Date is required") }
                        valid = false
                    }
                    if (!valid) return@Button

                    loading = true
                    uploading = true

                    coroutineScope.launch {
                        try {
                            val imageUrls = mutableListOf<String>()
                            if (images.isNotEmpty()) {
                                val storage = FirebaseStorage.getInstance().reference
                                val compressedUris = images.map { uri ->
                                    ImageUtils.compressImageIfNeeded(context, uri)
                                }
                                for ((index, uri) in compressedUris.withIndex()) {
                                    val ref = storage.child("transactions/${serial}_${System.currentTimeMillis()}_${index}.jpg")
                                    ref.putFile(uri).await()
                                    imageUrls += ref.downloadUrl.await().toString()
                                }
                            }

                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val parsedDate: Long = try {
                                sdf.parse(date)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }

                            val transaction = Transaction(
                                serial = serial,
                                model = model,
                                customerName = customerName,
                                phone = phone,
                                aadhaar = aadhaar,
                                amount = amountDouble ?: 0.0,
                                description = description,
                                date = date,
                                quantity = 1,
                                imageUrls = imageUrls,
                                type = type,
                                timestamp = System.currentTimeMillis()
                            )

                            val item = inventoryRepo.getItemBySerial(serial)

                            // Business rules
                            when (type) {
                                "Sale" -> {
                                    if (item == null || item.quantity < 1) {
                                        snackbarHostState.showSnackbar("Cannot sell: item not in inventory or insufficient stock.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                    if (!item.canSell()) {
                                        snackbarHostState.showSnackbar("Cannot sell: item is not available or in repair mode.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                }
                                "Purchase" -> {
                                    if (item != null) {
                                        snackbarHostState.showSnackbar("Cannot purchase: serial already exists in inventory.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                }
                                "Repair" -> {
                                    if (item == null) {
                                        snackbarHostState.showSnackbar("Cannot repair: serial not in inventory.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                    if (item.status != ItemStatus.AVAILABLE) {
                                        snackbarHostState.showSnackbar("Cannot repair: item must be available.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                    // Update item status to REPAIR
                                    val updatedItem = item.copy(status = ItemStatus.REPAIR)
                                    inventoryRepo.addOrUpdateItem(serial, updatedItem)
                                }
                                "Repair Return" -> {
                                    if (item == null) {
                                        snackbarHostState.showSnackbar("Cannot return: serial not in inventory.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                    if (item.status != ItemStatus.REPAIR) {
                                        snackbarHostState.showSnackbar("Cannot return: item must be in repair.")
                                        loading = false
                                        uploading = false
                                        return@launch
                                    }
                                    // Update item status back to AVAILABLE
                                    val updatedItem = item.copy(status = ItemStatus.AVAILABLE)
                                    inventoryRepo.addOrUpdateItem(serial, updatedItem)
                                }
                            }

                            val result = inventoryRepo.addTransaction(serial, transaction)
                            if (result is Result.Success) {
                                if (type == "Purchase") {
                                    val newItem = InventoryItem(
                                        serial = serial,
                                        name = model,
                                        model = model,
                                        quantity = 1,
                                        phone = phone,
                                        aadhaar = aadhaar,
                                        description = description,
                                        date = date,
                                        timestamp = System.currentTimeMillis(),
                                        imageUrls = imageUrls
                                    )
                                    inventoryRepo.addOrUpdateItem(serial, newItem)
                                }
                                if (type == "Sale" && item != null) {
                                    val updatedQty = item.quantity - 1
                                    val updatedItem = item.copy(quantity = updatedQty.coerceAtLeast(0))
                                    inventoryRepo.addOrUpdateItem(serial, updatedItem)
                                }

                                loading = false
                                uploading = false
                                showSuccess.value = true
                                snackbarHostState.showSnackbar("Transaction saved successfully!")
                                serial = ""
                                model = ""
                                customerName = ""
                                phone = ""
                                aadhaar = ""
                                amount = ""
                                description = ""
                                images = emptyList()
                            } else if (result is Result.Error) {
                                loading = false
                                uploading = false
                                snackbarHostState.showSnackbar(result.exception?.message ?: "Error saving transaction.")
                            }
                        } catch (e: Exception) {
                            loading = false
                            uploading = false
                            snackbarHostState.showSnackbar(e.message ?: "Unknown error occurred")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(28.dp)),
                enabled = canEdit && !loading && !uploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (loading || uploading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save Transaction", style = MaterialTheme.typography.titleMedium)
            }

            AnimatedVisibility(visible = showSuccess.value) {
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