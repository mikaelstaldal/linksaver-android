package nu.staldal.linksaver.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
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
    onEditItem: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onSettings: () -> Unit
) {
    val settings by repository.settingsFlow.collectAsState(initial = AppSettings("", "", ""))
    var items by remember { mutableStateOf<List<Link>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshLinks() {
        scope.launch {
            val api = repository.getApi(settings)
            if (api != null) {
                isRefreshing = true
                try {
                    items = api.getLinks(searchTerm.takeIf { it.isNotBlank() })
                } catch (e: Exception) {
                    Log.w("LinkListScreen", "Error fetching links: ${e.message}", e)
                    snackbarHostState.showSnackbar("Error fetching links: ${e.message}")
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    fun onDeleteItem(
        scope: CoroutineScope,
        repository: LinkRepository,
        settings: AppSettings,
        link: Link,
        snackbarHostState: SnackbarHostState
    ) {
        scope.launch {
            val api = repository.getApi(settings)
            if (api != null) {
                try {
                    api.deleteLink(link.ID)
                    refreshLinks()
                } catch (e: Exception) {
                    Log.w("LinkListScreen", "Error deleting link: ${e.message}", e)
                    snackbarHostState.showSnackbar("Error deleting link: ${e.message}")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        refreshLinks()
        onDispose { }
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
                    IconButton(onClick = { refreshLinks() }, enabled = !isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
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
                items(items) { link ->
                    if (isNote(link)) {
                        NoteItem(
                            link = link,
                            onEdit = { onEditItem(link.ID) },
                            onDelete = { onDeleteItem(scope, repository, settings, link, snackbarHostState) },
                        )
                    } else {
                        LinkItem(
                            link = link,
                            onClick = { onOpenLink(link.URL) },
                            onEdit = { onEditItem(link.ID) },
                            onDelete = { onDeleteItem(scope, repository, settings, link, snackbarHostState) },
                        )
                    }
                }
            }
        }
    }
}

fun isNote(link: Link) = link.URL.startsWith("note:")

@Composable
fun LinkItem(
    link: Link,
    onClick: () -> Unit,
    onEdit: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = link.Title, style = MaterialTheme.typography.titleMedium)
                Text(text = link.URL, style = MaterialTheme.typography.bodySmall)
                link.Description.let {
                    if (it.isNotBlank()) {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    link: Link,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = link.Title, style = MaterialTheme.typography.titleMedium)
                link.Description.let {
                    if (it.isNotBlank()) {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}
