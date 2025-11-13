package com.example.inventoryapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.inventoryapp.data.AuthRepository
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, authRepo: AuthRepository) {
    // Animation states
    var visible by remember { mutableStateOf(false) }
    
    // Scale animation for logo
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Rotation animation for continuous spin
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    LaunchedEffect(Unit) {
        visible = true
        delay(2000) // Show splash for 2 seconds
        
        if (authRepo.isLoggedIn()) {
            navController.navigate("inventory") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated logo with scale and subtle rotation
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(800)) + 
                            scaleIn(
                                initialScale = 0.3f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                ) {
                    Text(
                        text = "📦",
                        fontSize = 72.sp,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .scale(scale)
                    )
                }
                
                // Animated title with slide in from bottom
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = tween(600, delayMillis = 200)
                    ) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(600, delayMillis = 200)
                    )
                ) {
                    Text(
                        text = "Inventory App",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Animated subtitle with fade in
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = tween(600, delayMillis = 400)
                    ) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(600, delayMillis = 400)
                    )
                ) {
                    Text(
                        text = "Managing your inventory with ease",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )
                }
                
                // Animated progress indicator
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(
                        animationSpec = tween(400, delayMillis = 600)
                    ) + scaleIn(
                        animationSpec = tween(400, delayMillis = 600)
                    )
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}