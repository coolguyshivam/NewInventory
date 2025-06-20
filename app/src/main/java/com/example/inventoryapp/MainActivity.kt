package com.example.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.AuthRepository
import com.example.inventoryapp.data.InventoryRepository
import com.example.inventoryapp.model.UserRole
import com.example.inventoryapp.ui.navigation.AppNavHost
import com.example.inventoryapp.ui.theme.InventoryAppTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ShowChart
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InventoryAppTheme {
                val navController = rememberNavController()
                val authRepo = AuthRepository()
                val inventoryRepo = InventoryRepository()
                val userRole: UserRole = authRepo.getCurrentUserRole()
                Scaffold(
                    bottomBar = { BottomBar(navController, userRole) }
                ) { innerPadding ->
                    AppNavHost(
                        authRepo = authRepo,
                        inventoryRepo = inventoryRepo,
                        navController = navController,
                        userRole = userRole,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomBar(navController: NavController, userRole: UserRole) {
    val items = buildList {
        add(BottomNavItem("inventory", "Inventory", Icons.AutoMirrored.Filled.List))
        add(BottomNavItem("transaction", "Transaction", Icons.Filled.SwapHoriz))
        add(BottomNavItem("transaction_history", "Transaction History", Icons.Filled.Receipt))
        if (userRole == UserRole.ADMIN) {
            add(BottomNavItem("analytics", "Analytics", Icons.Filled.ShowChart))
        }
    }
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