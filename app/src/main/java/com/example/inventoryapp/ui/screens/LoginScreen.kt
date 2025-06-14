package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.Result
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun LoginScreen(navController: NavController, authRepo: AuthRepository) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                loading = true
                error = null
                // CoroutineScope omitted for brevity, use viewModel or LaunchedEffect in real app
            },
            enabled = !loading
        ) {
            Text("Login")
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (loading) CircularProgressIndicator()
    }
}