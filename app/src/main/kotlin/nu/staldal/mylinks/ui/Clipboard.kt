package nu.staldal.mylinks.ui

import android.content.ClipData
import android.content.Context
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry

suspend fun Clipboard.setPlainText(label: String, text: String) {
    setClipEntry(ClipData.newPlainText(label, text).toClipEntry())
}

suspend fun Clipboard.getPlainText(context: Context): String? =
    getClipEntry()?.clipData
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.takeIf { it.isNotEmpty() }
