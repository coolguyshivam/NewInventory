package com.example.inventoryapp.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.User
import com.example.inventoryapp.data.Result
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, authRepo: AuthRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showBiometric by remember { mutableStateOf(authRepo.isBiometricAvailable()) }
    
    // Animation states
    var contentVisible by remember { mutableStateOf(false) }
    
    // Scale animation for logo
    val scale by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo/Title with entrance animation
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(600)) + 
                    scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
        ) {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDCE6",
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Title with slide animation
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) + 
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(600, delayMillis = 100)
                    )
        ) {
            Text(
                text = "Inventory App",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Subtitle with fade in
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 200))
        ) {
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // Username Field with slide in
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 300)) + 
                    slideInVertically(
                        initialOffsetY = { it / 6 },
                        animationSpec = tween(500, delayMillis = 300)
                    )
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    error = null
                },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Password Field with slide in
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 400)) + 
                    slideInVertically(
                        initialOffsetY = { it / 6 },
                        animationSpec = tween(500, delayMillis = 400)
                    )
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = null
                },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Error Message with animation
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            error?.let { errorMessage ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Login Button with entrance animation
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 500)) + 
                    scaleIn(
                        initialScale = 0.9f,
                        animationSpec = tween(500, delayMillis = 500)
                    )
        ) {
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        error = "Please enter both username and password"
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        try {
                            val result: Result<User> = authRepo.login(username, password)
                            when (result) {
                                is Result.Success -> {
                                    authRepo.enableBiometricForUser(username)
                                    navController.navigate("inventory") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                is Result.Error -> {
                                    error = result.exception.message ?: "Login failed"
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Login failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Sign In", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Biometric Authentication Button
        if (showBiometric) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 600)) + 
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(500, delayMillis = 600)
                        )
            ) {
                OutlinedButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            authRepo.authenticateWithBiometric(
                                activity = activity,
                                onSuccess = {
                                    navController.navigate("inventory") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onError = { errorMessage ->
                                    error = errorMessage
                                }
                            )
                        } else {
                            error = "Biometric authentication not available"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                    )
                    Text(
                        "Use Fingerprint",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Demo Credentials Info
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(500, delayMillis = 700))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Demo Credentials:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Admin: admin / admin123\nOperator: operator / operator123\nViewer: viewer / viewer123",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}