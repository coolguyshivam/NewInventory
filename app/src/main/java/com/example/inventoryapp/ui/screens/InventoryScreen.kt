package com.example.inventoryapp.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.inventoryapp.model.InventoryFilters
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.InventoryViewModel
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.utils.downloadImageToGallery
import com.example.inventoryapp.ui.components.InventoryCard
import com.example.inventoryapp.ui.screens.AddEditItemDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.AnimatedVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: InventoryViewModel,
    inventoryRepo: com.example.inventoryapp.data.InventoryRepository
) {
    val context = LocalContext.current
    var filterText by remember { mutableStateOf("") }
    val availableInventory by viewModel.availableInventory.observeAsState(emptyList())
    val repairInventory by viewModel.repairInventory.observeAsState(emptyList())
    val inventory by viewModel.inventory.observeAsState(emptyList())
    val loading by viewModel.loading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val filters by viewModel.filters.observeAsState(InventoryFilters())
    val role = viewModel.userRole
    val sortBy by viewModel.sortBy.collectAsState()

    // Tab state: 0 = Inventory (Available), 1 = Repair
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedSerials by remember { mutableStateOf(setOf<String>()) }
    
    // Display list based on selected tab
    val displayInventory = when (selectedTabIndex) {
        0 -> availableInventory
        1 -> repairInventory
        else -> availableInventory
    }
    val allSelected = displayInventory.isNotEmpty() && displayInventory.all { selectedSerials.contains(it.serial) }

    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var filterDialogVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // States for delete confirmation dialog
    var deleteDialogVisible by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    var deleteReason by remember { mutableStateOf("") }
    var isProcessingDelete by remember { mutableStateOf(false) }
    
    // States for repair confirmation dialog
    var repairDialogVisible by remember { mutableStateOf(false) }
    var itemToRepair by remember { mutableStateOf<InventoryItem?>(null) }
    var repairReason by remember { mutableStateOf("") }
    var mechanicName by remember { mutableStateOf("") }
    var isProcessingRepair by remember { mutableStateOf(false) }

    var showPhotoViewer by remember { mutableStateOf(false) }
    var photoViewerImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var photoViewerStartIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }

    var lastInventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    LaunchedEffect(inventory) { lastInventory = inventory }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            val result = inventoryRepo.getAllItems(limit = 100)
            if (result is com.example.inventoryapp.data.Result.Success && result.data != lastInventory) {
                viewModel.loadInventory()
            }
        }
    }

    val scannedSerialLive = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("scannedSerial")
    val scannedSerialState = scannedSerialLive?.observeAsState()
    val scannedSerial = scannedSerialState?.value
    LaunchedEffect(scannedSerial) {
        scannedSerial?.let { serial ->
            filterText = serial
            viewModel.searchInventory(serial)
            viewModel.updateSerialFilter(serial)
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scannedSerial")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = filterText,
                onValueChange = {
                    filterText = it
                    viewModel.searchInventory(it)
                },
                placeholder = { Text("Search inventory...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { filterDialogVisible = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        IconButton(onClick = { navController.navigate("barcode_reader") }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode")
                        }
                        IconButton(onClick = { viewModel.loadInventory() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))

            // Tab selector for Inventory (Available) and Repair
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Inventory (${availableInventory.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Repair (${repairInventory.size})") }
                )
            }
            Spacer(Modifier.height(8.dp))

            when {
                loading == true -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
                displayInventory.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selectedTabIndex == 0) "No inventory items found." else "No items in repair.")
                }
                else -> LazyColumn {
                    itemsIndexed(displayInventory, key = { _, item -> item.serial }) { _, item ->
                        InventoryCard(
                            item = item,
                            userRole = role,
                            onClick = { selectedItem = item },
                            onEdit = { editingItem = item },
                            onDelete = {
                                if (!item.canDelete()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Cannot delete item: ensure item is available or in repair status")
                                    }
                                } else {
                                    itemToDelete = item
                                    deleteReason = ""
                                    deleteDialogVisible = true
                                }
                            },
                            onAddTransaction = { 
                                // Navigate to transaction screen with prefilled data
                                navController.navigate("transaction_screen?type=Sale&serial=${item.serial}&model=${item.model}")
                            },
                            onViewHistory = { 
                                // Navigate to transaction history filtered by serial
                                navController.navigate("transaction_history/${item.serial}")
                            },
                            onArchive = { /* archive not used */ },
                            onMarkRepair = {
                                if (!item.canMarkRepair()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Cannot mark item as repair: item must be available")
                                    }
                                } else {
                                    itemToRepair = item
                                    repairReason = ""
                                    mechanicName = ""
                                    repairDialogVisible = true
                                }
                            },
                            onReturn = {
                                scope.launch {
                                    if (!item.canReturn()) {
                                        snackbarHostState.showSnackbar("Cannot return item: item must be in repair or sold")
                                        return@launch
                                    }
                                    val updatedItem = item.copy(status = com.example.inventoryapp.model.ItemStatus.AVAILABLE)
                                    val result = inventoryRepo.addOrUpdateItem(item.serial, updatedItem)
                                    if (result is com.example.inventoryapp.data.Result.Success) {
                                        viewModel.loadInventory()
                                        snackbarHostState.showSnackbar("Item returned to available")
                                    } else if (result is com.example.inventoryapp.data.Result.Error) {
                                        snackbarHostState.showSnackbar(result.exception?.message ?: "Failed to return item")
                                    }
                                }
                            },
                            onSelectionChange = { checked: Boolean ->
                                selectedSerials = if (checked) selectedSerials + item.serial else selectedSerials - item.serial
                            },
                            isSelected = selectedSerials.contains(item.serial),
                            imageUrls = item.imageUrls,
                            onImageClick = { imgIdx: Int ->
                                photoViewerImages = item.imageUrls
                                photoViewerStartIndex = imgIdx
                                showPhotoViewer = true
                                zoom = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )
                    }
                }
            }

            selectedItem?.let { item ->
                var showPrice by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { 
                        selectedItem = null
                        showPrice = false
                    },
                    title = { Text(item.name, style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Serial Number: ${item.serial}", style = MaterialTheme.typography.bodyMedium)
                            Text("Model: ${item.model}", style = MaterialTheme.typography.bodyMedium)
                            Text("Purchase Date: ${item.date}", style = MaterialTheme.typography.bodyMedium)
                            Text("Quantity: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                            Text("Description: ${if (item.description.isNotBlank()) item.description else "N/A"}", style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(Modifier.height(8.dp))
                            Text("Customer Details:", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Name: ${if (item.customerName.isNotBlank()) item.customerName else "N/A"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Mobile Number: ${if (item.phone.isNotBlank()) item.phone else "N/A"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Aadhaar Number: ${if (item.aadhaar.isNotBlank()) item.aadhaar else "N/A"}", style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Purchase Price: ${if (showPrice) "₹${item.purchasePrice}" else "₹******"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                IconButton(onClick = { showPrice = !showPrice }) {
                                    Icon(
                                        imageVector = if (showPrice) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (showPrice) "Hide Price" else "Show Price"
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { 
                            selectedItem = null
                            showPrice = false
                        }) { Text("Close") }
                    }
                )
            }

            AnimatedVisibility(visible = showPhotoViewer && photoViewerImages.isNotEmpty()) {
                Dialog(onDismissRequest = { showPhotoViewer = false }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoomChange, _ ->
                                    zoom = (zoom * zoomChange).coerceIn(1f, 4f)
                                    offsetX += pan.x
                                    offsetY += pan.y
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photoViewerImages.getOrNull(photoViewerStartIndex),
                            contentDescription = "Inventory Image",
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = zoom,
                                    scaleY = zoom,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                        )

                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            photoViewerImages.forEachIndexed { idx, _ ->
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(if (photoViewerStartIndex == idx) Color.White else Color.Gray, CircleShape)
                                        .clickable { photoViewerStartIndex = idx }
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                downloading = true
                                val url = photoViewerImages.getOrNull(photoViewerStartIndex)
                                url?.let {
                                    scope.launch {
                                        downloadImageToGallery(
                                            context = context,
                                            url = it,
                                            fileName = "inventory_image_${System.currentTimeMillis()}.jpg",
                                            onDownloadComplete = {
                                                downloading = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Image downloaded to gallery!")
                                                }
                                            },
                                            onDownloadError = { errorMsg ->
                                                downloading = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Failed to download image: $errorMsg")
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            enabled = !downloading
                        ) {
                            if (downloading) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            else Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                        }
                        IconButton(
                            onClick = { showPhotoViewer = false },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            if (filterDialogVisible) {
                AlertDialog(
                    onDismissRequest = { filterDialogVisible = false },
                    title = { Text("Filter Inventory", style = MaterialTheme.typography.headlineSmall) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = filters.serial ?: "",
                                onValueChange = { viewModel.setFilters(filters.copy(serial = it)) },
                                label = { Text("Serial Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = filters.model ?: "",
                                onValueChange = { viewModel.setFilters(filters.copy(model = it)) },
                                label = { Text("Model") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = filters.quantity?.toString() ?: "",
                                onValueChange = { value: String ->
                                    val q = value.toIntOrNull()
                                    viewModel.setFilters(filters.copy(quantity = q))
                                },
                                label = { Text("Quantity") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = filters.date ?: "",
                                onValueChange = { viewModel.setFilters(filters.copy(date = it)) },
                                label = { Text("Date (yyyy-MM-dd)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    viewModel.setFilters(InventoryFilters())
                                    filterDialogVisible = false
                                }
                            ) { Text("Clear") }
                            Button(
                                onClick = { filterDialogVisible = false },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Apply") }
                        }
                    }
                )
            }
            
            // Edit Item Dialog
            editingItem?.let { item ->
                AddEditItemDialog(
                    originalItem = item,
                    onDismiss = { editingItem = null },
                    onSave = { updatedItem, changesSummary ->
                        scope.launch {
                            // Log the edit transaction
                            val editResult = inventoryRepo.createEditTransaction(
                                serial = item.serial,
                                item = updatedItem,
                                editedBy = "Admin", // TODO: Get actual user from auth context
                                changesSummary = changesSummary
                            )
                            
                            if (editResult is com.example.inventoryapp.data.Result.Success) {
                                // Update the item
                                val updateResult = inventoryRepo.addOrUpdateItem(item.serial, updatedItem)
                                if (updateResult is com.example.inventoryapp.data.Result.Success) {
                                    viewModel.loadInventory()
                                    snackbarHostState.showSnackbar("Item updated successfully")
                                    editingItem = null
                                } else if (updateResult is com.example.inventoryapp.data.Result.Error) {
                                    snackbarHostState.showSnackbar(updateResult.exception?.message ?: "Failed to update item")
                                }
                            } else if (editResult is com.example.inventoryapp.data.Result.Error) {
                                snackbarHostState.showSnackbar("Failed to log edit: ${editResult.exception?.message}")
                            }
                        }
                    }
                )
            }
            
            // Delete Confirmation Dialog
            if (deleteDialogVisible && itemToDelete != null) {
                AlertDialog(
                    onDismissRequest = { 
                        deleteDialogVisible = false
                        itemToDelete = null
                        deleteReason = ""
                        isProcessingDelete = false
                    },
                    title = { Text("Delete Item: ${itemToDelete?.serial}") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Please provide a reason for deleting this item:", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = deleteReason,
                                onValueChange = { deleteReason = it },
                                label = { Text("Reason for deletion") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (isProcessingDelete) return@Button  // Prevent multiple clicks
                                isProcessingDelete = true
                                
                                val item = itemToDelete ?: return@Button
                                scope.launch {
                                    // First create DELETE transaction with reason, then delete item
                                    val deleteResult = inventoryRepo.createDeleteTransaction(
                                        serial = item.serial,
                                        item = item,
                                        deletedBy = "Admin", // TODO: Get actual user from auth context
                                        reason = deleteReason
                                    )
                                    
                                    if (deleteResult is com.example.inventoryapp.data.Result.Success) {
                                        val result = inventoryRepo.deleteItem(item.serial)
                                        if (result is com.example.inventoryapp.data.Result.Success) {
                                            viewModel.loadInventory()
                                            snackbarHostState.showSnackbar("Item deleted")
                                            deleteDialogVisible = false
                                            itemToDelete = null
                                            deleteReason = ""
                                            isProcessingDelete = false
                                        } else if (result is com.example.inventoryapp.data.Result.Error) {
                                            snackbarHostState.showSnackbar(result.exception?.message ?: "Delete failed!")
                                            isProcessingDelete = false
                                        }
                                    } else if (deleteResult is com.example.inventoryapp.data.Result.Error) {
                                        snackbarHostState.showSnackbar("Failed to log deletion: ${deleteResult.exception?.message}")
                                        isProcessingDelete = false
                                    }
                                }
                            },
                            enabled = deleteReason.isNotBlank() && !isProcessingDelete
                        ) {
                            if (isProcessingDelete) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Delete")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            deleteDialogVisible = false
                            itemToDelete = null
                            deleteReason = ""
                            isProcessingDelete = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Repair Confirmation Dialog
            if (repairDialogVisible && itemToRepair != null) {
                AlertDialog(
                    onDismissRequest = { 
                        repairDialogVisible = false
                        itemToRepair = null
                        repairReason = ""
                        mechanicName = ""
                        isProcessingRepair = false
                    },
                    title = { Text("Mark Item for Repair: ${itemToRepair?.serial}") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Please provide details for marking this item as in repair:", style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = repairReason,
                                onValueChange = { repairReason = it },
                                label = { Text("Reason for repair") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                            OutlinedTextField(
                                value = mechanicName,
                                onValueChange = { mechanicName = it },
                                label = { Text("Mechanic name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (isProcessingRepair) return@Button  // Prevent multiple clicks
                                isProcessingRepair = true
                                
                                val item = itemToRepair ?: return@Button
                                scope.launch {
                                    val updatedItem = item.copy(status = com.example.inventoryapp.model.ItemStatus.REPAIR)
                                    
                                    // Create a repair transaction with reason and mechanic name
                                    // Note: Using customerName field to store mechanic name for repair transactions
                                    val repairTransaction = com.example.inventoryapp.model.Transaction(
                                        id = "",
                                        type = "REPAIR",
                                        model = item.model,
                                        serial = item.serial,
                                        customerName = mechanicName, // Mechanic name stored in customerName field
                                        phoneNumber = "",
                                        aadhaarNumber = "",
                                        amount = 0.0,
                                        quantity = 1,
                                        description = "Marked for repair. Reason: $repairReason",
                                        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                        timestamp = System.currentTimeMillis(),
                                        userRole = "Admin", // TODO: Get actual user from auth context
                                        images = emptyList()
                                    )
                                    
                                    val txResult = inventoryRepo.addTransaction(item.serial, repairTransaction)
                                    if (txResult is com.example.inventoryapp.data.Result.Success) {
                                        val result = inventoryRepo.addOrUpdateItem(item.serial, updatedItem)
                                        if (result is com.example.inventoryapp.data.Result.Success) {
                                            viewModel.loadInventory()
                                            snackbarHostState.showSnackbar("Item marked as in repair")
                                            repairDialogVisible = false
                                            itemToRepair = null
                                            repairReason = ""
                                            mechanicName = ""
                                            isProcessingRepair = false
                                        } else if (result is com.example.inventoryapp.data.Result.Error) {
                                            snackbarHostState.showSnackbar(result.exception?.message ?: "Failed to mark as repair")
                                            isProcessingRepair = false
                                        }
                                    } else if (txResult is com.example.inventoryapp.data.Result.Error) {
                                        snackbarHostState.showSnackbar("Failed to log repair: ${txResult.exception?.message}")
                                        isProcessingRepair = false
                                    }
                                }
                            },
                            enabled = repairReason.isNotBlank() && mechanicName.isNotBlank() && !isProcessingRepair
                        ) {
                            if (isProcessingRepair) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("Mark for Repair")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            repairDialogVisible = false
                            itemToRepair = null
                            repairReason = ""
                            mechanicName = ""
                            isProcessingRepair = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}