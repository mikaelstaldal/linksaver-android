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
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nu.staldal.linksaver.R
import nu.staldal.linksaver.data.AppSettings
import nu.staldal.linksaver.data.ItemRepository
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkScreen(
    repository: ItemRepository,
    onBack: () -> Unit
) {
    val settings by repository.settingsFlow.collectAsState(initial = AppSettings("", "", ""))
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val linkAlreadyExistsMessage = stringResource(R.string.link_already_exists)
    val clipboardManager = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_link)) },
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
                enabled = true,
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.let { url = it.text }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                }
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val api = repository.getApi(settings)
                        if (api != null) {
                            try {
                                api.addLink(url)
                                onBack()
                            } catch (e: HttpException) {
                                if (e.code() == 409) {
                                    snackbarHostState.showSnackbar(linkAlreadyExistsMessage)
                                } else {
                                    Log.w("AddLinkScreen", "Error saving link: ${e.message}", e)
                                    snackbarHostState.showSnackbar( "Error saving link: ${e.message}")
                                }
                            } catch (e: Exception) {
                                Log.w("AddLinkScreen", "Error saving link: ${e.message}", e)
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
