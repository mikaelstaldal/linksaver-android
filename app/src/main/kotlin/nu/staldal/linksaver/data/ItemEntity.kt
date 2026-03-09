package nu.staldal.linksaver.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE
}

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val description: String,
    val addedAt: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

fun ItemEntity.toItem() = Item(
    ID = id,
    URL = url,
    Title = title,
    Description = description,
    AddedAt = addedAt,
)

fun Item.toEntity(syncStatus: SyncStatus = SyncStatus.SYNCED) = ItemEntity(
    id = ID,
    url = URL,
    title = Title,
    description = Description,
    addedAt = AddedAt,
    syncStatus = syncStatus,
)
