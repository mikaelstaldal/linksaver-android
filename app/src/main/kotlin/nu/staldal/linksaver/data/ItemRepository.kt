package nu.staldal.linksaver.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nu.staldal.linksaver.BuildConfig
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val baseUrl: String,
    val username: String,
    val password: String
)

class ItemRepository(private val context: Context) {

    private val LEGACY_KEY_BASE_URL = stringPreferencesKey("base_url")
    private val LEGACY_KEY_USERNAME = stringPreferencesKey("username")
    private val LEGACY_KEY_PASSWORD = stringPreferencesKey("password")

    private val credentialStore = CredentialStore.getInstance(context)
    private val dao = AppDatabase.getInstance(context).itemDao()
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _settingsFlow = MutableStateFlow(credentialStore.toAppSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            migrateCredentialsIfNeeded()
            _settingsFlow.value = credentialStore.toAppSettings()
        }
    }

    private suspend fun migrateCredentialsIfNeeded() {
        if (credentialStore.hasCredentials()) return
        val prefs = context.dataStore.data.first()
        val baseUrl = prefs[LEGACY_KEY_BASE_URL]
        val username = prefs[LEGACY_KEY_USERNAME]
        val password = prefs[LEGACY_KEY_PASSWORD]
        if (!baseUrl.isNullOrBlank() || !username.isNullOrBlank() || !password.isNullOrBlank()) {
            credentialStore.save(baseUrl ?: "", username ?: "", password ?: "")
            context.dataStore.edit {
                it.remove(LEGACY_KEY_BASE_URL)
                it.remove(LEGACY_KEY_USERNAME)
                it.remove(LEGACY_KEY_PASSWORD)
            }
        }
    }

    val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    suspend fun saveSettings(settings: AppSettings) {
        credentialStore.save(settings.baseUrl, settings.username, settings.password)
        _settingsFlow.value = settings
    }

    private fun getApi(): ItemApi? {
        val baseUrl = credentialStore.baseUrl
        if (baseUrl.isNullOrBlank()) return null
        val username = credentialStore.username ?: ""
        val password = credentialStore.password ?: ""

        val authToken = Credentials.basic(username, password)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", authToken)
                    .build()
                chain.proceed(request)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                        redactHeader("Authorization")
                    })
                }
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ItemApi::class.java)
    }

    fun getItems(searchTerm: String): Flow<List<Item>> {
        val flow = if (searchTerm.isBlank()) dao.getAllItems() else dao.searchItems(searchTerm)
        return flow.map { entities -> entities.map { it.toItem() } }
    }

    suspend fun getItemById(id: String): Item? = dao.getItemById(id)?.toItem()

    suspend fun addLink(url: String) {
        val tempId = "temp-${UUID.randomUUID()}"
        val entity = ItemEntity(
            id = tempId,
            url = url,
            title = url,
            description = "",
            addedAt = Instant.now().toString(),
            syncStatus = SyncStatus.PENDING_CREATE
        )
        dao.upsertItem(entity)
        trySyncAndEnqueue()
    }

    suspend fun addNote(title: String, text: String) {
        val tempId = "temp-${UUID.randomUUID()}"
        val entity = ItemEntity(
            id = tempId,
            url = "note:$title",
            title = title,
            description = text,
            addedAt = Instant.now().toString(),
            syncStatus = SyncStatus.PENDING_CREATE
        )
        dao.upsertItem(entity)
        trySyncAndEnqueue()
    }

    suspend fun updateItem(id: String, title: String, description: String) {
        val existing = dao.getItemById(id) ?: return
        val newStatus = if (existing.syncStatus == SyncStatus.PENDING_CREATE) {
            SyncStatus.PENDING_CREATE
        } else {
            SyncStatus.PENDING_UPDATE
        }
        dao.upsertItem(existing.copy(title = title, description = description, syncStatus = newStatus))
        trySyncAndEnqueue()
    }

    suspend fun deleteItem(id: String) {
        val existing = dao.getItemById(id) ?: return
        if (existing.syncStatus == SyncStatus.PENDING_CREATE) {
            dao.deleteItem(id)
        } else {
            dao.markForDeletion(id)
            trySyncAndEnqueue()
        }
    }

    suspend fun syncToServer() {
        val api = getApi() ?: return
        val pending = dao.getPendingChanges()

        for (item in pending) {
            try {
                when (item.syncStatus) {
                    SyncStatus.PENDING_CREATE -> {
                        try {
                            val updatedList = if (item.url.startsWith("note:")) {
                                api.addNote(item.title, item.description)
                            } else {
                                api.addLink(item.url)
                            }
                            dao.deleteItem(item.id)
                            dao.replaceAllSynced(updatedList.map { it.toEntity() })
                        } catch (e: HttpException) {
                            dao.deleteItem(item.id)
                            if (e.code() != 409) {
                                throw e
                            }
                        }
                    }

                    SyncStatus.PENDING_UPDATE -> {
                        api.updateItem(item.id, item.title, item.description)
                        dao.upsertItem(item.copy(syncStatus = SyncStatus.SYNCED))
                    }

                    SyncStatus.PENDING_DELETE -> {
                        api.deleteItem(item.id)
                        dao.deleteItem(item.id)
                    }

                    SyncStatus.SYNCED -> { /* no-op */
                    }
                }
            } catch (e: HttpException) {
                val errorMessage = e.response()?.errorBody()?.string()
                Log.w(
                    "ItemRepository",
                    "Sync failed for item ${item.id}: HTTP ${e.code()} ${e.message()} - $errorMessage"
                )
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, errorMessage ?: "Sync failed: HTTP ${e.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.w("ItemRepository", "Sync failed for item ${item.id}: ${e.message}", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun refreshFromServer() {
        val api = getApi() ?: return
        val serverItems = api.getItems()
        dao.replaceAllSynced(serverItems.map { it.toEntity() })
    }

    private fun isConnectedNow(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private suspend fun trySyncAndEnqueue() {
        if (isConnectedNow()) {
            try {
                syncToServer()
            } catch (e: Exception) {
                Log.d("ItemRepository", "Immediate sync failed, will retry via WorkManager: ${e.message}")
            }
        }
        enqueueSyncWork(context)
    }

    companion object {
        fun enqueueSyncWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
