package com.example.inventoryapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.inventoryapp.model.Transaction

@Composable
fun TransactionHistoryCard(
    transaction: Transaction,
    onClick: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    // Smooth scale animation for interactions
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(scale)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp
        ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Model and Serial - Primary info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.model,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (backgroundColor == Color(0xFFE53E3E)) 
                        Color.White else MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = transaction.type,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (backgroundColor == Color(0xFFE53E3E))
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Serial number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Serial: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.serial,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Amount and Date in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Amount: ${transaction.amount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White.copy(alpha = 0.9f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show deletion info for DELETE transactions
            if (transaction.type.uppercase() == "DELETE" && transaction.deletedInfo != null) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Surface(
                    color = if (backgroundColor == Color(0xFFE53E3E))
                        Color.White.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Deleted by: ${transaction.deletedInfo.deletedBy} at ${transaction.deletedInfo.deletedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (backgroundColor == Color(0xFFE53E3E))
                            Color.White
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}