package nu.staldal.mylinks.data

data class Item(
    val ID: String,
    val URL: String,
    val Title: String,
    val Description: String,
    val AddedAt: String,
) {
    fun isNote() = URL.startsWith("note:")
}
