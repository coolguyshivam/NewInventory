package com.example.inventoryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.data.Result
import com.example.inventoryapp.model.Transaction
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(inventoryRepo: InventoryRepository) {
    val transactions = remember { mutableStateListOf<Transaction>() }

    // Load transactions once
    LaunchedEffect(Unit) {
        val result = inventoryRepo.getAllTransactions()
        if (result is Result.Success) {
            transactions.clear()
            transactions.addAll(result.data)
        }
    }

    // Generate filter options
    val types = remember(transactions) { listOf("All") + transactions.map { it.type }.distinct().sorted() }
    val models = remember(transactions) { listOf("All") + transactions.mapNotNull { it.model }.distinct().sorted() }

    // Filter state
    var selectedType by remember { mutableStateOf("All") }
    var selectedModel by remember { mutableStateOf("All") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    // Filtering logic
    val filtered = transactions.filter { tx ->
        (selectedType == "All" || tx.type.equals(selectedType, ignoreCase = true)) &&
        (selectedModel == "All" || (tx.model?.equals(selectedModel, ignoreCase = true) == true)) &&
        (minAmount.toDoubleOrNull()?.let { tx.amount >= it } ?: true) &&
        (maxAmount.toDoubleOrNull()?.let { tx.amount <= it } ?: true) &&
        (startDate.isBlank() || tx.date >= startDate) &&
        (endDate.isBlank() || tx.date <= endDate)
    }

    // Totals by type for visuals
    val totalsByType = filtered.groupBy { it.type }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
    val typeColors = listOf(
        Color(0xFF4CAF50), // Sale
        Color(0xFF2196F3), // Purchase
        Color(0xFFFFC107), // Return
        Color(0xFFF44336)  // Repair
    )
    val typeColorMap = types.filter { it != "All" }.mapIndexed { idx, type -> type to typeColors.getOrElse(idx) { Color.Gray } }.toMap()

    val totalAmount = filtered.sumOf { it.amount }
    val totalSales = filtered.filter { it.type.equals("Sale", true) }.sumOf { it.amount }
    val totalPurchases = filtered.filter { it.type.equals("Purchase", true) }.sumOf { it.amount }
    val totalReturns = filtered.filter { it.type.equals("Return", true) }.sumOf { it.amount }
    val totalRepairs = filtered.filter { it.type.equals("Repair", true) }.sumOf { it.amount }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics / Stats") }) }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            // --- FILTERS ---
            FilterRow(
                label = "Type",
                options = types,
                selected = selectedType,
                onSelected = { selectedType = it }
            )
            Spacer(Modifier.height(6.dp))
            FilterRow(
                label = "Product/Model",
                options = models,
                selected = selectedModel,
                onSelected = { selectedModel = it }
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { minAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Min ₹") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { maxAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Max ₹") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            // --- VISUAL SUMMARY CARDS ---
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard("Total", totalAmount, Color(0xFF1976D2))
                StatCard("Sales", totalSales, Color(0xFF4CAF50))
                StatCard("Purchases", totalPurchases, Color(0xFF2196F3))
                StatCard("Returns", totalReturns, Color(0xFFFFC107))
                StatCard("Repairs", totalRepairs, Color(0xFFF44336))
            }
            Spacer(Modifier.height(12.dp))
            // --- BAR CHART BY TYPE ---
            if (totalsByType.isNotEmpty()) {
                Text("Totals by Type", style = MaterialTheme.typography.titleSmall)
                BarChart(totalsByType, typeColorMap)
            }
            Spacer(Modifier.height(16.dp))

            // --- TRANSACTION LIST ---
            Text("Transactions: ${filtered.size}", style = MaterialTheme.typography.titleMedium)
            if (filtered.isEmpty()) {
                Text("No transactions match the filters.", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered.size) { idx ->
                        val tx = filtered[idx]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = typeColorMap[tx.type] ?: Color.LightGray
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${tx.type} | ₹${tx.amount} | ${tx.date}", style = MaterialTheme.typography.titleSmall)
                                Text("Model: ${tx.model ?: "-"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Refactored FilterRow ---
@Composable
fun FilterRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            modifier = Modifier.padding(end = 8.dp)
        )
        Box(Modifier.weight(1f)) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                label = null, // No internal label, label is on the left
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: Double, color: Color) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(70.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text("₹${value.toInt()}", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, Double>,
    colorMap: Map<String, Color>,
    maxBarWidth: Dp = 220.dp
) {
    val maxValue = max(data.values.maxOrNull() ?: 1.0, 1.0)
    Column(Modifier.fillMaxWidth()) {
        data.forEach { (type, amount) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .height(24.dp)
                        .width((amount / maxValue * maxBarWidth.value).dp)
                        .background(colorMap[type] ?: Color.Gray, shape = RoundedCornerShape(7.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text("$type: ₹${amount.toInt()}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}