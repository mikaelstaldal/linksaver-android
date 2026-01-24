package nu.staldal.linksaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nu.staldal.linksaver.ui.theme.LinksaverTheme
import nu.staldal.linksaver.data.LinkRepository
import androidx.compose.ui.platform.LocalContext

import nu.staldal.linksaver.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinksaverTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val repository = remember { LinkRepository(context) }
                
                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        LinkListScreen(
                            repository = repository,
                            onAddLink = { navController.navigate("add") },
                            onEditLink = { id -> navController.navigate("edit/$id") },
                            onSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("add") {
                        EditLinkScreen(
                            repository = repository,
                            linkId = null,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("edit/{id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")
                        EditLinkScreen(
                            repository = repository,
                            linkId = id,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
