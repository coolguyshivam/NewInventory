package com.example.inventoryapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.inventoryapp.model.InventoryItem
import com.example.inventoryapp.model.ItemStatus
import com.example.inventoryapp.model.UserRole
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag

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
    onMarkRepair: () -> Unit = {},
    onReturn: () -> Unit = {},
    onSelectionChange: ((Boolean) -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    imageUrls: List<String>,
    onImageClick: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Smooth scale animation for interactions
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val formattedDate = remember(item.date) {
        if (item.date.isNotEmpty()) item.date else "-"
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .scale(scale)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
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
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 8.dp else 4.dp,
                pressedElevation = 2.dp,
                hoveredElevation = 6.dp
            ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // --- IMAGES ---
            AnimatedVisibility(
                visible = imageUrls.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        imageUrls.forEachIndexed { idx, url ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Inventory image",
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(idx) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (userRole == UserRole.ADMIN || userRole == UserRole.OPERATOR) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert, 
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            // Mark as Repair (only if available)
                            if (item.canMarkRepair()) {
                                DropdownMenuItem(
                                    text = { Text("Mark as In Repair") },
                                    onClick = {
                                        showMenu = false
                                        onMarkRepair()
                                    }
                                )
                            }
                            
                            // Return action (if in repair or sold)
                            if (item.canReturn()) {
                                DropdownMenuItem(
                                    text = { Text("Return to Available") },
                                    onClick = {
                                        showMenu = false
                                        onReturn()
                                    }
                                )
                            }
                            
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    if (item.canDelete()) {
                                        onDelete()
                                    }
                                },
                                enabled = item.canDelete()
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
            
            Spacer(Modifier.height(12.dp))
            
            // Information rows with better styling
            InfoRow(label = "Serial", value = item.serial)
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Model", value = item.model)
            Spacer(Modifier.height(8.dp))
            InfoRow(label = "Date", value = formattedDate)
            
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(300)) + 
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) + 
                       shrinkVertically(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    
                    // Description with card background
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (item.description.isNotBlank()) item.description else "No description",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Action buttons with better spacing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (userRole == UserRole.ADMIN || userRole == UserRole.OPERATOR) {
                            Button(
                                onClick = onAddTransaction,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("addTransactionButton"),
                                enabled = item.canSell(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    "Add Transaction",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onViewHistory,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("historyButton"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "History",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        }
        
        // Repair Mode Badge Overlay with animation
        AnimatedVisibility(
            visible = item.status == ItemStatus.REPAIR || item.isInRepair,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .zIndex(1f)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "REPAIR MODE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
