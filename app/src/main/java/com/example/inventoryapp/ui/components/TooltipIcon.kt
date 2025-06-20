package com.example.inventoryapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TooltipIcon(tip: String) {
    var showTip by remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Help",
            modifier = Modifier
                .size(16.dp)
                .clickable { showTip = !showTip }
        )
        DropdownMenu(
            expanded = showTip,
            onDismissRequest = { showTip = false }
        ) {
            DropdownMenuItem(
                text = { Text(tip, color = Color.DarkGray) },
                onClick = { showTip = false }
            )
        }
    }
}