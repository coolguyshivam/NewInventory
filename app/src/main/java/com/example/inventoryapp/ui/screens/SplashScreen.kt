package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(navController: NavController, authRepo: AuthRepository) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            // Insert logic to navigate based on auth state
        }
    }
}