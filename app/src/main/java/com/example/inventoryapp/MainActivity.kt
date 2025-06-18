package com.example.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.ui.navigation.AppNavHost
import com.example.inventoryapp.ui.theme.InventoryAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InventoryAppTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BottomBar(navController) }
                ) { innerPadding ->
                    AppNavHost(
                        authRepo = AuthRepository(),
                        inventoryRepo = InventoryRepository(),
                        navController = navController
                    )
                }
            }
        }
    }
}

// Place this BottomBar composable in the same file or in a suitable location in your project
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun BottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("inventory", "Inventory", Icons.Default.List),
        BottomNavItem("transaction", "Transaction", Icons.Default.SwapHoriz),
        BottomNavItem("reports", "Reports", Icons.Default.Receipt)
    )
    NavigationBar {
        val navBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}