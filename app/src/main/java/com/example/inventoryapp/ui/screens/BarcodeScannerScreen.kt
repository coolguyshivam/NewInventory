package com.example.inventoryapp.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun BarcodeScannerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var launched by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent: Intent? = result.data
            val contents = intent?.getStringExtra("SCAN_RESULT")
            if (!contents.isNullOrBlank()) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("scannedSerial", contents)
                navController.popBackStack()
            } else {
                navController.popBackStack()
            }
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            val integrator = IntentIntegrator(context as Activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            integrator.setPrompt("Scan a barcode")
            integrator.setCameraId(0)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            launcher.launch(integrator.createScanIntent())
        }
    }

    Scaffold { paddingValues ->
        Surface(modifier = Modifier.padding(paddingValues)) {
            Text("Launching barcode scanner...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}