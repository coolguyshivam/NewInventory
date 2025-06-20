package com.example.inventoryapp.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.foundation.layout.padding
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.ui.components.TransactionForm
import com.example.inventoryapp.ui.components.TransactionTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository,
    userRole: UserRole,
    requiredFields: List<String> = listOf("serial", "model", "amount", "date")
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val showSuccess = remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TransactionTopBar(navController = navController)
        }
    ) { paddingValues ->
        TransactionForm(
            navController = navController,
            inventoryRepo = inventoryRepo,
            userRole = userRole,
            requiredFields = requiredFields,
            modifier = Modifier.padding(paddingValues),
            snackbarHostState = snackbarHostState,
            showSuccess = showSuccess
        )
    }
}