package org.flossware.jnexus.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import org.flossware.jnexus.android.ui.screens.*

/**
 * Main activity for the JNexus Android app.
 * <p>
 * Provides bottom navigation with 4 tabs:
 * - List: List components in repositories
 * - Search: Advanced search with filters
 * - Stats: Repository statistics
 * - Settings: Configure credentials and options
 * </p>
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as NexusApplication

        setContent {
            MaterialTheme {
                NexusApp(app)
            }
        }
    }
}

@Composable
fun NexusApp(app: NexusApplication) {
    val navController = rememberNavController()
    val currentRoute = remember { mutableStateOf("list") }

    // Listen to navigation changes
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute.value = destination.route ?: "list"
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, "List") },
                    label = { Text("List") },
                    selected = currentRoute.value == "list",
                    onClick = { navController.navigate("list") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, "Search") },
                    label = { Text("Search") },
                    selected = currentRoute.value == "search",
                    onClick = { navController.navigate("search") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, "Stats") },
                    label = { Text("Stats") },
                    selected = currentRoute.value == "stats",
                    onClick = { navController.navigate("stats") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute.value == "settings",
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "list",
            modifier = Modifier.padding(padding)
        ) {
            composable("list") { RepositoryListScreen(app) }
            composable("search") { SearchScreen(app) }
            composable("stats") { StatsScreen(app) }
            composable("settings") { SettingsScreen(app) }
        }
    }
}
