package com.example.inventoryapp.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable date picker field component
 * Returns date in yyyy-MM-dd format
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "yyyy-MM-dd"
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Parse current value to set initial date
    val initialDate = try {
        if (value.isNotBlank()) {
            dateFormat.parse(value)?.let { calendar.time = it }
        }
        calendar
    } catch (e: Exception) {
        Calendar.getInstance()
    }
    
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val formattedDate = dateFormat.format(calendar.time)
                onValueChange(formattedDate)
            },
            initialDate.get(Calendar.YEAR),
            initialDate.get(Calendar.MONTH),
            initialDate.get(Calendar.DAY_OF_MONTH)
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = { /* read-only, date selected via picker */ },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        trailingIcon = {
            IconButton(onClick = { if (enabled) datePickerDialog.show() }) {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date")
            }
        },
        readOnly = true,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { datePickerDialog.show() },
        shape = RoundedCornerShape(16.dp)
    )
}
