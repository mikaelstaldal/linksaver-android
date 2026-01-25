package nu.staldal.linksaver.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nu.staldal.linksaver.R
import nu.staldal.linksaver.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLinkScreen(
    repository: LinkRepository,
    linkId: String?,
    onBack: () -> Unit
) {
    val settings by repository.settingsFlow.collectAsState(initial = AppSettings("", "", ""))
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isEdit = linkId != null

    LaunchedEffect(linkId, settings) {
        if (isEdit) {
            val api = repository.getApi(settings)
            if (api != null) {
                try {
                    val link = api.getLink(linkId)
                    url = link.URL
                    title = link.Title
                    description = link.Description
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error fetching link: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEdit) R.string.edit_link else R.string.add_link)) },
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
                enabled = !isEdit, // URL cannot be edited according to API description (only PATCH {id} for title)
                trailingIcon = {
                    if (!isEdit) {
                        IconButton(onClick = {
                            clipboardManager.getText()?.let { url = it.text }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste))
                        }
                    }
                }
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth()
            )
            if (!isEdit) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val api = repository.getApi(settings)
                        if (api != null) {
                            try {
                                if (isEdit) {
                                    api.updateLink(linkId, title)
                                } else {
                                    api.addLink(url)
                                }
                                onBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error saving link: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        } else {
                            isLoading = false
                            snackbarHostState.showSnackbar("Settings not configured")
                        }
                    }
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
                enabled = !isLoading && url.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
