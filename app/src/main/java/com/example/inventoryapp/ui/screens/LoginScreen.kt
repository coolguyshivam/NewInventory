package com.example.inventoryapp.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.inventoryapp.data.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun LoginScreen(navController: NavHostController, authRepo: AuthRepository) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            loading = true
            error = null
            scope.launch {
                when (val res = authRepo.login(email, password)) {
                    is com.example.inventoryapp.data.Result.Success -> navController.navigate("inventory") { popUpTo("login") { inclusive = true } }
                    is com.example.inventoryapp.data.Result.Error -> error = res.exception.message
                    else -> {}
                }
                loading = false
            }
        }) { Text("Sign In") }
        TextButton(onClick = { navController.navigate("register") }) { Text("Register") }
    }
}