package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.Transaction
import com.example.inventoryapp.model.UserRole
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import com.example.inventoryapp.ui.components.DateField
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    inventoryRepo: InventoryRepository,
    userRole: UserRole,
    navController: androidx.navigation.NavController? = null
) {
    // Only allow admin users (not operators)
    if (userRole != UserRole.ADMIN) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Analytics available to admin accounts only.", color = Color.Red)
        }
        return
    }

    val transactions = remember { mutableStateListOf<Transaction>() }
    val firebaseAnalytics = FirebaseAnalytics.getInstance(LocalContext.current)

    // Log when the analytics screen is viewed
    LaunchedEffect(Unit) {
        firebaseAnalytics.logEvent("analytics_screen_viewed", null)
        val result = inventoryRepo.getAllTransactions()
        if (result is Result.Success) {
            transactions.clear()
            transactions.addAll(result.data)
        }
    }

    // Generate filter options - only include Purchase and Sale for analytics
    val types = remember(transactions) { 
        listOf("All", "Purchase", "Sale")
    }
    val models = remember(transactions) { 
        listOf("All") + transactions
            .filter { it.type.equals("Purchase", ignoreCase = true) || it.type.equals("Sale", ignoreCase = true) }
            .mapNotNull { it.model }
            .distinct()
            .sorted() 
    }

    // Filter state
    var selectedType by remember { mutableStateOf("All") }
    var selectedModel by remember { mutableStateOf("All") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    // Analytics: log filter changes
    LaunchedEffect(selectedType, selectedModel, minAmount, maxAmount, startDate, endDate) {
        val bundle = Bundle().apply {
            putString("type", selectedType)
            putString("model", selectedModel)
            putString("min_amount", minAmount)
            putString("max_amount", maxAmount)
            putString("start_date", startDate)
            putString("end_date", endDate)
        }
        firebaseAnalytics.logEvent("analytics_filter_changed", bundle)
    }

    // Filtering logic
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val startDateLong = startDate.takeIf { it.isNotBlank() }?.let { sdf.parse(it)?.time } ?: Long.MIN_VALUE
    val endDateLong = endDate.takeIf { it.isNotBlank() }?.let { sdf.parse(it)?.time } ?: Long.MAX_VALUE

    // Filter to show only Purchase and Sale transactions
    val filtered = transactions.filter { tx ->
        // Only include Purchase and Sale transactions
        (tx.type.equals("Purchase", ignoreCase = true) || tx.type.equals("Sale", ignoreCase = true)) &&
        (selectedType == "All" || tx.type.equals(selectedType, ignoreCase = true)) &&
        (selectedModel == "All" || tx.model.equals(selectedModel, ignoreCase = true)) &&
        (minAmount.toDoubleOrNull()?.let { tx.amount >= it } ?: true) &&
        (maxAmount.toDoubleOrNull()?.let { tx.amount <= it } ?: true) &&
        (tx.timestamp in startDateLong..endDateLong)
    }

    val totalSales = filtered.filter { it.type.equals("Sale", true) }.sumOf { it.amount }
    val totalPurchases = filtered.filter { it.type.equals("Purchase", true) }.sumOf { it.amount }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                actions = {
                    // Only show user management button for admins
                    if (navController != null && userRole == UserRole.ADMIN) {
                        IconButton(onClick = { navController.navigate("user_management") }) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Manage Users"
                            )
                        }
                    }
                }
            ) 
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Summary Cards Section
            Text(
                "Financial Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sales Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sales", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("₹$totalSales", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("${filtered.count { it.type.equals("Sale", true) }} txns", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
                
                // Purchases Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Purchases", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("₹$totalPurchases", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text("${filtered.count { it.type.equals("Purchase", true) }} txns", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            // Filters Section
            Text(
                "Filters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            AnalyticsFilters(
                types = types,
                models = models,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                selectedModel = selectedModel,
                onModelSelected = { selectedModel = it },
                minAmount = minAmount,
                onMinAmountChange = { minAmount = it },
                maxAmount = maxAmount,
                onMaxAmountChange = { maxAmount = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it }
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Transaction List Section
            Text(
                "Transactions (${filtered.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (filtered.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions match the selected filters", color = Color.Gray)
                    }
                }
            } else {
                filtered.forEach { tx ->
                    TransactionStatsCard(tx)
                }
            }
        }
    }
}

@Composable
fun AnalyticsFilters(
    types: List<String>,
    models: List<String>,
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    minAmount: String,
    onMinAmountChange: (String) -> Unit,
    maxAmount: String,
    onMaxAmountChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DropdownMenuBox(
            label = "Type",
            options = types,
            selectedOption = selectedType,
            onOptionSelected = onTypeSelected
        )
        Spacer(Modifier.width(8.dp))
        DropdownMenuBox(
            label = "Model",
            options = models,
            selectedOption = selectedModel,
            onOptionSelected = onModelSelected
        )
    }
    Spacer(Modifier.height(8.dp))
    Row {
        OutlinedTextField(
            value = minAmount,
            onValueChange = onMinAmountChange,
            label = { Text("Min Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = maxAmount,
            onValueChange = onMaxAmountChange,
            label = { Text("Max Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    Row {
        DateField(
            value = startDate,
            onValueChange = onStartDateChange,
            label = "Start Date",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        DateField(
            value = endDate,
            onValueChange = onEndDateChange,
            label = "End Date",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DropdownMenuBox(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .width(140.dp)
                .clickable { expanded = true },
            readOnly = true
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onOptionSelected(option)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun TransactionStatsCard(tx: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Type: ${tx.type} | Model: ${tx.model} | Amount: ₹${tx.amount}", fontWeight = FontWeight.Bold)
            Text("Serial: ${tx.serial}")
            Text("Date: ${tx.date}")
            Text("Customer: ${tx.customerName}")
        }
    }
}