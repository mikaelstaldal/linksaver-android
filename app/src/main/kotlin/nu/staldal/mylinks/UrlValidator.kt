package nu.staldal.mylinks

import java.net.URI
import java.net.URISyntaxException

/**
 * Validates and sanitizes text received from external sources (e.g. share intents)
 * before it is stored and synced as a link.
 */
object UrlValidator {

    /**
     * Extracts the first http/https URL from the given text.
     *
     * @return the sanitized URL, or null if the text does not contain a valid web URL.
     */
    fun sanitize(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.split(WHITESPACE).firstOrNull { isWebUrl(it) }
    }

    private fun isWebUrl(candidate: String): Boolean {
        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: URISyntaxException) {
            false
        }
    }

    private val WHITESPACE = Regex("\\s+")
}
