package com.example.quickqr.navigation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.quickqr.databinding.FragmentHomeBinding
import com.example.quickqr.ui.history.HistoryScreen
import com.example.quickqr.ui.result.ResultScreen
import com.example.quickqr.ui.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Scan.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home Screen with HomeFragment
        composable(Screen.Scan.route) {
            AndroidViewBinding(
                factory = FragmentHomeBinding::inflate,
                modifier = Modifier.fillMaxSize()
            ) { binding ->
                // Handle any binding setup if needed
            }
        }

        // History Screen
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToResult = { content, type ->
                    navController.navigate(
                        Screen.Result.createRoute(content, type)
                    )
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        // Result Screen
        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("qrContent") { type = NavType.StringType },
                navArgument("qrType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val content = backStackEntry.arguments?.getString("qrContent") ?: ""
            val type = backStackEntry.arguments?.getString("qrType") ?: ""

            // You can uncomment this section once you have a ResultScreen composable
            // ResultScreen(
            //     content = content,
            //     type = type,
            //     onBack = { navController.popBackStack() }
            // )
        }
    }
}