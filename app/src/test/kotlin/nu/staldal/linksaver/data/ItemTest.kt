package nu.staldal.linksaver.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemTest {

    @Test
    fun testIsNote() {
        val linkItem = Item(
            ID = "1",
            URL = "https://example.com",
            Title = "Example",
            Description = "An example link",
            AddedAt = "2023-01-01T00:00:00Z"
        )
        assertFalse(linkItem.isNote())

        val noteItem = Item(
            ID = "2",
            URL = "note:This%20is%20a%20note",
            Title = "A note",
            Description = "A note description",
            AddedAt = "2023-01-01T00:00:00Z"
        )
        assertTrue(noteItem.isNote())
    }
}
