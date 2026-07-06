package nu.staldal.mylinks.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import nu.staldal.mylinks.R
import nu.staldal.mylinks.data.Item
import nu.staldal.mylinks.data.ItemRepository

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
    var isRefreshing by remember { mutableStateOf(false) }
    var searchTerm by remember { mutableStateOf("") }
    val items by repository.getItems(searchTerm).collectAsState(initial = emptyList())
    val isConnected by repository.isConnected.collectAsState(initial = false)
    val pendingSyncCount by repository.pendingSyncCount.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var itemPendingDeletion by remember { mutableStateOf<Item?>(null) }

    fun refreshLinks() {
        scope.launch {
            isRefreshing = true
            try {
                if (!repository.refreshFromServer()) {
                    snackbarHostState.showSnackbar(context.getString(R.string.server_url_not_configured))
                }
            } catch (e: Exception) {
                Log.w("ListScreen", "Error fetching items: ${e.message}", e)
                snackbarHostState.showSnackbar(context.getString(R.string.error_fetching_items, e.message))
            } finally {
                isRefreshing = false
            }
        }
    }

    fun onDeleteItem(item: Item) {
        scope.launch {
            try {
                repository.deleteItem(item.ID)
            } catch (e: Exception) {
                Log.w("ListScreen", "Error deleting item: ${e.message}", e)
                snackbarHostState.showSnackbar(context.getString(R.string.error_deleting_item, e.message))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { refreshLinks() }, enabled = isConnected && !isRefreshing) {
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
            if (pendingSyncCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pending_sync, pendingSyncCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = searchTerm,
                onValueChange = { searchTerm = it },
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.TopCenter,
                ) {
                    Text(
                        text = if (searchTerm.isBlank()) stringResource(R.string.no_items_yet)
                        else stringResource(R.string.no_search_results, searchTerm),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn {
                    items(items) { item ->
                        if (item.isNote()) {
                            NoteItem(
                                item = item,
                                onEdit = { onEditItem(item.ID) },
                                onDelete = { itemPendingDeletion = item },
                            )
                        } else {
                            LinkItem(
                                item = item,
                                onClick = { onOpenLink(item.URL) },
                                onEdit = { onEditItem(item.ID) },
                                onDelete = { itemPendingDeletion = item },
                            )
                        }
                    }
                }
            }
        }

        itemPendingDeletion?.let { item ->
            AlertDialog(
                onDismissRequest = { itemPendingDeletion = null },
                title = { Text(stringResource(R.string.delete_confirmation_title)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteItem(item)
                        itemPendingDeletion = null
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemPendingDeletion = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
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
