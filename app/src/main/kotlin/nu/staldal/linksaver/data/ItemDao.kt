package nu.staldal.linksaver.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE syncStatus != 'PENDING_DELETE' ORDER BY addedAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query(
        "SELECT * FROM items WHERE syncStatus != 'PENDING_DELETE' " +
                "AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') " +
                "ORDER BY addedAt DESC"
    )
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: String): ItemEntity?

    @Upsert
    suspend fun upsertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ItemEntity>)

    @Query("UPDATE items SET syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markForDeletion(id: String)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("SELECT * FROM items WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingChanges(): List<ItemEntity>

    @Transaction
    suspend fun replaceAllSynced(items: List<ItemEntity>) {
        deleteAllSynced()
        upsertItems(items)
    }

    @Query("DELETE FROM items WHERE syncStatus = 'SYNCED'")
    suspend fun deleteAllSynced()
}
