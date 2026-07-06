package nu.staldal.mylinks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nu.staldal.mylinks.data.ItemRepository

class ShareActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val url = intent.getStringExtra(Intent.EXTRA_TEXT)?.let { UrlValidator.sanitize(it) }
            if (url != null) {
                saveUrl(url)
            } else {
                Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    private fun saveUrl(url: String) {
        val repository = ItemRepository(this)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.addLink(url)
                }
                Toast.makeText(this@ShareActivity, R.string.link_saved, Toast.LENGTH_SHORT).show()
                ItemRepository.enqueueSyncWork(this@ShareActivity)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ShareActivity,
                    getString(R.string.error_saving_link, e.message ?: e.javaClass.simpleName),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finish()
            }
        }
    }
}
