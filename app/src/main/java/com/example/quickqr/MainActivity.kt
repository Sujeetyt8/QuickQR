package com.example.quickqr

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.navigation.compose.rememberNavController
import com.example.quickqr.navigation.NavGraph
import com.example.quickqr.ui.theme.QuickQRTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            QuickQRTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    
                    // Check for first run and request permissions if needed
                    var isFirstRun by remember { mutableStateOf(true) }
                    
                    val permissions = mutableListOf(Manifest.permission.CAMERA).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    
                    val permissionsState = rememberMultiplePermissionsState(permissions)
                    
                    LaunchedEffect(Unit) {
                        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        isFirstRun = prefs.getBoolean("isFirstRun", true)
                        
                        if (isFirstRun) {
                            permissionsState.launchMultiplePermissionRequest()
                            prefs.edit { putBoolean("isFirstRun", false) }
                        }
                    }
                    
                    // Show a message if permissions are not granted
                    if (!permissionsState.allPermissionsGranted) {
                        LaunchedEffect(Unit) {
                            if (!isFirstRun) {
                                Toast.makeText(
                                    context,
                                    "Some features may not work without required permissions",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    
                    // Main app content
                    NavGraph(
                        navController = navController,
                        startDestination = "scan"
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    QuickQRTheme {
        Greeting("Android")
    }
}