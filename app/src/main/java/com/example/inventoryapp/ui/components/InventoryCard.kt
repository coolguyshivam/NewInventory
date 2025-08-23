package com.example.inventoryapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.InventoryStatus
import com.example.inventoryapp.model.UserRole
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryCard(
    item: InventoryItem,
    userRole: UserRole,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddTransaction: () -> Unit,
    onViewHistory: () -> Unit,
    onArchive: () -> Unit = {},
    onSelectionChange: ((Boolean) -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    onImageClick: (Int) -> Unit,
    onStatusChange: ((InventoryStatus) -> Unit)? = null // New parameter for status changes
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val currentStatus = item.getCurrentStatus()

    val formattedDate = remember(item.date) {
        if (item.date.isNotEmpty()) item.date else "-"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .combinedClickable(
                onClick = {
                    expanded = !expanded
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    onSelectionChange?.let { it(!isSelected) }
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                },
                role = Role.Button
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- IMAGES ---
                if (imageUrls.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        imageUrls.forEachIndexed { idx, url ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Inventory image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(end = 8.dp)
                                    .clickable { onImageClick(idx) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (userRole == UserRole.ADMIN || userRole == UserRole.OPERATOR) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        if (userRole == UserRole.ADMIN) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                onClick = {
                                    showMenu = false
                                    onArchive()
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(text = "Serial: ${item.serial}")
            Text(text = "Model: ${item.model}")
            Text(text = "Date: $formattedDate")
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(text = if (item.description.isNotBlank()) item.description else "No description")
                
                // Status controls
                if (userRole == UserRole.ADMIN || userRole == UserRole.OPERATOR) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Status Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Repair Mode button
                        Button(
                            onClick = { 
                                if (currentStatus == InventoryStatus.REPAIR) {
                                    // Return from repair to available
                                    onStatusChange?.invoke(InventoryStatus.AVAILABLE)
                                } else {
                                    // Move to repair mode
                                    onStatusChange?.invoke(InventoryStatus.REPAIR)
                                }
                            },
                            enabled = currentStatus == InventoryStatus.AVAILABLE || currentStatus == InventoryStatus.REPAIR,
                            colors = if (currentStatus == InventoryStatus.REPAIR) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (currentStatus == InventoryStatus.REPAIR) "Exit Repair" else "Repair Mode",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        // Return button (for sold or repair items)
                        if (currentStatus == InventoryStatus.SOLD || currentStatus == InventoryStatus.REPAIR) {
                            Button(
                                onClick = { onStatusChange?.invoke(InventoryStatus.AVAILABLE) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Return", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                }
                
                // Transaction buttons
                Row {
                    if (userRole == UserRole.ADMIN || userRole == UserRole.OPERATOR) {
                        Button(
                            onClick = onAddTransaction,
                            enabled = currentStatus.canBeSold(), // Disable if in repair mode
                            modifier = Modifier.alpha(if (currentStatus.canBeSold()) 1f else 0.6f)
                        ) {
                            Icon(
                                Icons.Default.Sell,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add Transaction")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(onClick = onViewHistory) {
                        Text("History")
                    }
                }
                
                // Show message when action is disabled
                if (!currentStatus.canBeSold() && (userRole == UserRole.ADMIN || userRole == UserRole.OPERATOR)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Item cannot be sold while in ${currentStatus.getDisplayName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Status badge
            if (currentStatus != InventoryStatus.AVAILABLE) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = currentStatus.getDisplayColor()
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = currentStatus.getDisplayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}