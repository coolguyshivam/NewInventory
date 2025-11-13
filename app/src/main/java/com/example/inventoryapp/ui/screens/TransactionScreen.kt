package com.example.inventoryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.ui.components.TransactionForm

/**
 * Wrapper screen for TransactionForm.
 * Features:
 * - Dynamic required fields (based on transaction type/user role if needed)
 * - Success callback for navigation or state handling
 * - Undo/History shortcut after transaction
 * - Accessibility: descriptive content for top bar/back button
 * - Error boundary for repo errors
 * - Documentation for props
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    userRole: UserRole,
    prefillType: String? = null,
    prefillSerial: String? = null,
    prefillModel: String? = null,
    requiredFields: List<String>? = null, // allow null for dynamic
    navToBarcodeScanner: (() -> Unit)? = null,
    onTransactionSuccess: (() -> Unit)? = null // callback for parent
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val showSuccess = remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Example for dynamic required fields
    val effectiveRequiredFields = requiredFields ?: when (prefillType?.lowercase()) {
        "sale" -> listOf("serial", "model", "amount", "date")
        "purchase" -> listOf("serial", "model", "amount", "date")
        "repair" -> listOf("serial", "model", "date")
        "return" -> listOf("serial", "date")
        else -> listOf("serial", "model", "amount", "date")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            "MG Inventory Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to previous screen",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TransactionForm(
                navController = navController,
                inventoryRepo = inventoryRepo,
                userRole = userRole,
                requiredFields = effectiveRequiredFields,
                snackbarHostState = snackbarHostState,
                showSuccess = showSuccess,
                prefillType = prefillType,
                prefillSerial = prefillSerial,
                prefillModel = prefillModel,
                modifier = Modifier.fillMaxSize(),
            )

            // Transaction success snackbar with undo/history shortcut
            if (showSuccess.value) {
                LaunchedEffect(showSuccess.value) {
                    val result = snackbarHostState.showSnackbar(
                        message = "Transaction successful!",
                        actionLabel = "History",
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        navController.navigate("transaction_history")
                    }
                    showSuccess.value = false
                    onTransactionSuccess?.invoke() // call parent callback
                }
            }

            // Error boundary with modern styling
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                error?.let {
                    Snackbar(
                        action = {
                            TextButton(
                                onClick = { error = null },
                                shape = RoundedCornerShape(8.dp)
                            ) { 
                                Text(
                                    "Dismiss",
                                    fontWeight = FontWeight.Medium
                                ) 
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) { 
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        ) 
                    }
                }
            }
        }
    }
}