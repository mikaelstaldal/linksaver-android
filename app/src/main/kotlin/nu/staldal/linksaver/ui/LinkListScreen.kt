package nu.staldal.linksaver.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nu.staldal.linksaver.R
import nu.staldal.linksaver.data.AppSettings
import nu.staldal.linksaver.data.Link
import nu.staldal.linksaver.data.LinkRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkListScreen(
    repository: LinkRepository,
    onAddLink: () -> Unit,
    onEditLink: (String) -> Unit,
    onSettings: () -> Unit
) {
    val settings by repository.settingsFlow.collectAsState(initial = AppSettings("", "", ""))
    var links by remember { mutableStateOf<List<Link>>(emptyList()) }
    var searchTerm by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshLinks() {
        scope.launch {
            val api = repository.getApi(settings)
            if (api != null) {
                try {
                    links = api.getLinks(searchTerm.takeIf { it.isNotBlank() })
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error fetching links: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(settings, searchTerm) {
        refreshLinks()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLink) {
                Icon(Icons.Default.Add, contentDescription = "Add Link")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            LazyColumn {
                items(links) { link ->
                    LinkItem(
                        link = link,
                        onClick = { onEditLink(link.ID) },
                        onDelete = {
                            scope.launch {
                                val api = repository.getApi(settings)
                                if (api != null) {
                                    try {
                                        api.deleteLink(link.ID)
                                        refreshLinks()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error deleting link: ${e.message}")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LinkItem(
    link: Link,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = link.Title, style = MaterialTheme.typography.titleMedium)
                Text(text = link.URL, style = MaterialTheme.typography.bodySmall)
                link.Description?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
