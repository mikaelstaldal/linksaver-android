package nu.staldal.linksaver.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nu.staldal.linksaver.R
import nu.staldal.linksaver.data.AppSettings
import nu.staldal.linksaver.data.ItemRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    repository: ItemRepository,
    itemId: String,
    onBack: () -> Unit
) {
    val settings by repository.settingsFlow.collectAsState(initial = AppSettings("", "", ""))
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(itemId, settings) {
        val api = repository.getApi(settings)
        if (api != null) {
            try {
                val link = api.getItem(itemId)
                url = link.URL
                title = link.Title
                description = link.Description
            } catch (e: Exception) {
                Log.w("EditScreen", "Error fetching item: ${e.message}", e)
                snackbarHostState.showSnackbar(context.getString(R.string.error_fetching_item, e.message))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_item)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.url)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false, // URL cannot be edited according to API description (only PATCH {id} for title and description)
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val api = repository.getApi(settings)
                        if (api != null) {
                            try {
                                api.updateItem(itemId, title, description)
                                onBack()
                            } catch (e: Exception) {
                                Log.w("EditScreen", "Error fetching link: ${e.message}", e)
                                snackbarHostState.showSnackbar(context.getString(R.string.error_saving_link, e.message))
                            } finally {
                                isLoading = false
                            }
                        } else {
                            isLoading = false
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_not_configured))
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading && url.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
