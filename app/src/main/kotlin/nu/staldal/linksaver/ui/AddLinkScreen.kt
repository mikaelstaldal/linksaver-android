package nu.staldal.linksaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import nu.staldal.linksaver.UrlValidator
import nu.staldal.linksaver.data.ItemRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkScreen(
    repository: ItemRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    val sanitizedUrl = UrlValidator.sanitize(url)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_link)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            sanitizedUrl?.let { link ->
                                scope.launch {
                                    repository.addLink(link)
                                    onBack()
                                }
                            }
                        },
                        enabled = sanitizedUrl != null
                    ) {
                        Text(stringResource(R.string.save))
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
                isError = url.isNotBlank() && sanitizedUrl == null,
                supportingText = {
                    if (url.isNotBlank() && sanitizedUrl == null) {
                        Text(stringResource(R.string.invalid_url))
                    }
                },
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.let { url = it.text }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                }
            )
        }
    }
}
