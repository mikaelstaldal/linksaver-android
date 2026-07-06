package nu.staldal.mylinks.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException
import androidx.core.content.edit

class CredentialStore private constructor(private val prefs: SharedPreferences) {

    val baseUrl: String?
        get() = prefs.getString(KEY_BASE_URL, null)

    val username: String?
        get() = prefs.getString(KEY_USERNAME, null)

    val password: String?
        get() = prefs.getString(KEY_PASSWORD, null)

    fun save(baseUrl: String, username: String, password: String) {
        prefs.edit {
            putString(KEY_BASE_URL, baseUrl)
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
        }
    }

    fun hasCredentials(): Boolean =
        !baseUrl.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()

    fun toAppSettings() = AppSettings(
        baseUrl = baseUrl ?: "",
        username = username ?: "",
        password = password ?: ""
    )

    companion object {
        private const val PREFS_FILE_NAME = "secure_credentials"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"

        @Volatile
        private var instance: CredentialStore? = null

        fun getInstance(context: Context): CredentialStore =
            instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }

        private fun create(context: Context): CredentialStore {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return CredentialStore(
                try {
                    buildEncryptedPrefs(context, masterKey)
                } catch (_: GeneralSecurityException) {
                    context.deleteSharedPreferences(PREFS_FILE_NAME)
                    buildEncryptedPrefs(context, masterKey)
                } catch (_: IOException) {
                    context.deleteSharedPreferences(PREFS_FILE_NAME)
                    buildEncryptedPrefs(context, masterKey)
                }
            )
        }

        private fun buildEncryptedPrefs(
            context: Context,
            masterKey: MasterKey
        ): SharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
