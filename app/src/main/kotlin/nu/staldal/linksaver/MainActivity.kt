package nu.staldal.linksaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nu.staldal.linksaver.data.ItemRepository
import nu.staldal.linksaver.ui.AddLinkScreen
import nu.staldal.linksaver.ui.AddNoteScreen
import nu.staldal.linksaver.ui.EditScreen
import nu.staldal.linksaver.ui.ListScreen
import nu.staldal.linksaver.ui.SettingsScreen
import nu.staldal.linksaver.ui.theme.LinksaverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinksaverTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val repository = remember { ItemRepository(context) }

                val onOpenLink: (String) -> Unit = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                }

                NavHost(navController = navController, startDestination = "list") {
                    composable("list") {
                        ListScreen(
                            repository = repository,
                            onAddLink = { navController.navigate("add-link") },
                            onAddNote = { navController.navigate("add-note") },
                            onEditItem = { id -> navController.navigate("edit/$id") },
                            onOpenLink = onOpenLink,
                            onSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("add-link") {
                        AddLinkScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("add-note") {
                        AddNoteScreen(
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("edit/{id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")!!
                        EditScreen(
                            repository = repository,
                            itemId = id,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
