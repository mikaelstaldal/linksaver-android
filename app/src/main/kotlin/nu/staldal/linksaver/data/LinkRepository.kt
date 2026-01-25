package nu.staldal.linksaver.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val baseUrl: String,
    val username: String,
    val password: String
)

class LinkRepository(private val context: Context) {

    private val KEY_BASE_URL = stringPreferencesKey("base_url")
    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_PASSWORD = stringPreferencesKey("password")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            baseUrl = preferences[KEY_BASE_URL] ?: "",
            username = preferences[KEY_USERNAME] ?: "",
            password = preferences[KEY_PASSWORD] ?: ""
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = settings.baseUrl
            preferences[KEY_USERNAME] = settings.username
            preferences[KEY_PASSWORD] = settings.password
        }
    }

    fun getApi(settings: AppSettings): LinkApi? {
        if (settings.baseUrl.isBlank()) return null

        val authToken = Credentials.basic(settings.username, settings.password)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", authToken)
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor {
                Log.d("LinkRepository", "Request: ${it.request().url} ${it.request().method} ${it.request().headers}")
                val response = it.proceed(it.request())
                Log.d("LinkRepository", "Response: ${response.code} ${response.headers}")
                response
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(if (settings.baseUrl.endsWith("/")) settings.baseUrl else "${settings.baseUrl}/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(LinkApi::class.java)
    }
}
