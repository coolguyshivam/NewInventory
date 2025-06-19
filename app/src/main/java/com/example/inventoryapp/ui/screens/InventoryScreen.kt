package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.ui.components.InventoryCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    authRepo: AuthRepository
) {
    val inventory by inventoryRepo.getInventoryFlow().collectAsState(initial = emptyList())
    val groupedInventory = remember(inventory) { inventory.groupBy { it.name } }
    var selectedName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (inventory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(groupedInventory.entries.toList()) { (name, itemsForName) ->
                    // Show a summary card for the name or the first item
                    InventoryCard(
                        item = itemsForName.first(),
                        onClick = {
                            selectedName = name
                            showSheet = true
                        }
                    )
                }
            }
        }
    }

    // Bottom Sheet showing all items for selected name
    if (showSheet && selectedName != null) {
        val itemsForName = groupedInventory[selectedName!!] ?: emptyList()
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Items for: $selectedName", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                itemsForName.forEach { item ->
                    InventoryCard(item = item)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}