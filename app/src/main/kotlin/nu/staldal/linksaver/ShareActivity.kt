package nu.staldal.linksaver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nu.staldal.linksaver.data.ItemRepository

class ShareActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                saveUrl(sharedText)
            } ?: finish()
        } else {
            finish()
        }
    }

    private fun saveUrl(url: String) {
        val repository = ItemRepository(this)
        scope.launch {
            withContext(Dispatchers.IO) {
                repository.addLink(url)
            }
            Toast.makeText(this@ShareActivity, R.string.link_saved, Toast.LENGTH_SHORT).show()
            ItemRepository.enqueueSyncWork(this@ShareActivity)
            finish()
        }
    }
}
