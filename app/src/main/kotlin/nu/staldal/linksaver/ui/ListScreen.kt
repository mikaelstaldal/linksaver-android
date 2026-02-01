package nu.staldal.linksaver.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AddLink
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nu.staldal.linksaver.R
import nu.staldal.linksaver.data.AppSettings
import nu.staldal.linksaver.data.Item
import nu.staldal.linksaver.data.ItemRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    repository: ItemRepository,
    onAddLink: () -> Unit,
    onAddNote: () -> Unit,
    onEditItem: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val settings by repository.settingsFlow.collectAsState(initial = AppSettings("", "", ""))
    var items by remember { mutableStateOf<List<Item>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    fun refreshLinks() {
        scope.launch {
            val api = repository.getApi(settings)
            if (api != null) {
                isRefreshing = true
                try {
                    items = api.getItems(searchTerm.takeIf { it.isNotBlank() })
                } catch (e: Exception) {
                    Log.w("ListScreen", "Error fetching items: ${e.message}", e)
                    snackbarHostState.showSnackbar(context.getString(R.string.error_fetching_items, e.message))
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    fun onDeleteItem(
        scope: CoroutineScope,
        repository: ItemRepository,
        settings: AppSettings,
        item: Item,
        snackbarHostState: SnackbarHostState
    ) {
        scope.launch {
            val api = repository.getApi(settings)
            if (api != null) {
                try {
                    api.deleteItem(item.ID)
                    refreshLinks()
                } catch (e: Exception) {
                    Log.w("ListScreen", "Error deleting item: ${e.message}", e)
                    snackbarHostState.showSnackbar(context.getString(R.string.error_deleting_item, e.message))
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
            Column {
                FloatingActionButton(onClick = onAddLink) {
                    Icon(Icons.Filled.AddLink, contentDescription = stringResource(R.string.add_link))
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = onAddNote) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = stringResource(R.string.add_note))
                }
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
                items(items) { item ->
                    if (item.isNote()) {
                        NoteItem(
                            item = item,
                            onEdit = { onEditItem(item.ID) },
                            onDelete = { onDeleteItem(scope, repository, settings, item, snackbarHostState) },
                        )
                    } else {
                        LinkItem(
                            item = item,
                            onClick = { onOpenLink(item.URL) },
                            onEdit = { onEditItem(item.ID) },
                            onDelete = { onDeleteItem(scope, repository, settings, item, snackbarHostState) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinkItem(
    item: Item,
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
                Text(text = item.Title, style = MaterialTheme.typography.titleMedium)
                Text(text = item.URL, style = MaterialTheme.typography.bodySmall)
                item.Description.let {
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
    item: Item,
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
                Text(text = item.Title, style = MaterialTheme.typography.titleMedium)
                item.Description.let {
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
