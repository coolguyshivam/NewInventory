package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    inventoryRepo: InventoryRepository
) {
    var searchText by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("All") }
    val transactionList by inventoryRepo.getAllTransactionsFlow().collectAsState(initial = emptyList())

    // State to track which transaction is expanded (by serial or unique id)
    var expandedSerial by remember { mutableStateOf<String?>(null) }

    // Barcode scan integration for search
    val scannedSerial = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.get<String>("scannedSerial")
    LaunchedEffect(scannedSerial) {
        if (!scannedSerial.isNullOrBlank()) {
            searchText = scannedSerial
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scannedSerial")
        }
    }

    // Filtering logic
    val filteredTransactions = remember(transactionList, searchText, filterType) {
        transactionList
            .filter {
                (filterType == "All" || it.type.equals(filterType, ignoreCase = true)) &&
                (searchText.isBlank() ||
                        it.serial.contains(searchText, true) ||
                        it.model.contains(searchText, true) ||
                        it.type.contains(searchText, true) ||
                        it.description.contains(searchText, true) ||
                        it.date.contains(searchText, true) ||
                        it.amount.toString().contains(searchText, true)
                )
            }
            .sortedByDescending { parseDate(it.date) }
    }

    Scaffold(
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { navController.navigate("barcode_scanner") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search any detail") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                singleLine = true,
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Clear search")
                        }
                    }
                }
            )
            // Filter Row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter: ", style = MaterialTheme.typography.bodyMedium)
                FilterChip(
                    selected = filterType == "All",
                    onClick = { filterType = "All" },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = filterType == "Purchase",
                    onClick = { filterType = "Purchase" },
                    label = { Text("Purchase") },
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = filterType == "Sale",
                    onClick = { filterType = "Sale" },
                    label = { Text("Sale") }
                )
            }

            // Transactions list
            if (filteredTransactions.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found.")
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp)
                ) {
                    items(filteredTransactions, key = { it.serial + it.type }) { tx ->
                        // Only expand by serial (so clicking any purchase or sale of that serial expands both cards)
                        val isExpanded = expandedSerial == tx.serial
                        TransactionReportCard(
                            transaction = tx,
                            onClick = { expandedSerial = if (isExpanded) null else tx.serial }
                        )
                        if (isExpanded) {
                            // Show the two red cards: one for purchase, one for sale
                            val purchase = findTransactionByType(transactionList, tx.serial, "Purchase")
                            val sale = findTransactionByType(transactionList, tx.serial, "Sale")

                            RedTransactionDetailCard(
                                transaction = purchase,
                                title = "Purchase Details"
                            )
                            RedTransactionDetailCard(
                                transaction = sale,
                                title = "Sale Details"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionReportCard(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val cardColor = when (transaction.type.lowercase()) {
        "purchase" -> Color(0xFFB9F6CA) // Green
        "sale" -> Color(0xFFB3E5FC)     // Blue
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = transaction.type,
                style = MaterialTheme.typography.titleMedium,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(4.dp))
            Text("Model: ${transaction.model}", style = MaterialTheme.typography.bodyMedium)
            Text("Serial: ${transaction.serial}", style = MaterialTheme.typography.bodyMedium)
            Text("Amount: ${transaction.amount}", style = MaterialTheme.typography.bodyMedium)
            Text("Date: ${transaction.date}", style = MaterialTheme.typography.bodyMedium)
            if (transaction.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(transaction.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RedTransactionDetailCard(
    transaction: Transaction?,
    title: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)) // Light red
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red
            )
            if (transaction != null) {
                Spacer(Modifier.height(4.dp))
                Text("Model: ${transaction.model}", style = MaterialTheme.typography.bodyMedium)
                Text("Serial: ${transaction.serial}", style = MaterialTheme.typography.bodyMedium)
                Text("Amount: ${transaction.amount}", style = MaterialTheme.typography.bodyMedium)
                Text("Date: ${transaction.date}", style = MaterialTheme.typography.bodyMedium)
                if (transaction.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(transaction.description, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("No details available.", color = Color.Gray)
            }
        }
    }
}

fun findTransactionByType(
    allTransactions: List<Transaction>,
    serial: String,
    type: String
): Transaction? {
    // Find the first transaction with matching serial and type, latest by date
    return allTransactions
        .filter { it.serial == serial && it.type.equals(type, ignoreCase = true) }
        .maxByOrNull { parseDate(it.date) }
}

fun parseDate(dateString: String): Date {
    return try {
        SimpleDateFormat("d/M/yyyy", Locale.getDefault()).parse(dateString) ?: Date(0)
    } catch (e: Exception) {
        Date(0)
    }
}