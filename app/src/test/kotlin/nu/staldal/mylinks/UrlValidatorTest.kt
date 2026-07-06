package nu.staldal.mylinks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun acceptsHttpsUrl() {
        assertEquals("https://example.com", UrlValidator.sanitize("https://example.com"))
    }

    @Test
    fun acceptsHttpUrl() {
        assertEquals("http://example.com/path?q=1", UrlValidator.sanitize("http://example.com/path?q=1"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("https://example.com", UrlValidator.sanitize("  https://example.com\n"))
    }

    @Test
    fun extractsUrlFromSurroundingText() {
        assertEquals("https://example.com", UrlValidator.sanitize("Check this out https://example.com now"))
    }

    @Test
    fun rejectsPlainText() {
        assertNull(UrlValidator.sanitize("just some text"))
    }

    @Test
    fun rejectsEmpty() {
        assertNull(UrlValidator.sanitize("   "))
    }

    @Test
    fun rejectsNoteScheme() {
        assertNull(UrlValidator.sanitize("note:evil"))
    }

    @Test
    fun rejectsJavascriptScheme() {
        assertNull(UrlValidator.sanitize("javascript:alert(1)"))
    }

    @Test
    fun rejectsFileScheme() {
        assertNull(UrlValidator.sanitize("file:///etc/passwd"))
    }

    @Test
    fun rejectsSchemeWithoutHost() {
        assertNull(UrlValidator.sanitize("https://"))
    }
}
