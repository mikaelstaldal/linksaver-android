package nu.staldal.linksaver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nu.staldal.linksaver.data.ItemRepository
import retrofit2.HttpException

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
            try {
                val settings = repository.settingsFlow.first()
                val api = repository.getApi(settings)
                if (api != null) {
                    withContext(Dispatchers.IO) {
                        api.addLink(url)
                    }
                    Toast.makeText(this@ShareActivity, R.string.link_saved, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ShareActivity, R.string.settings_not_configured, Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    Toast.makeText(this@ShareActivity, R.string.link_already_exists, Toast.LENGTH_LONG).show()
                } else {
                    Log.w("ShareActivity", "Error saving link: ${e.message}", e)
                    Toast.makeText(
                        this@ShareActivity,
                        getString(R.string.error_saving_link, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.w("ShareActivity", "Error saving link: ${e.message}", e)
                Toast.makeText(
                    this@ShareActivity,
                    getString(R.string.error_saving_link, e.message),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finish()
            }
        }
    }
}
