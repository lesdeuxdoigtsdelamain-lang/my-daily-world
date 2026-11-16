package uk.family.mydailyworld

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "editions") data class CachedEdition(@PrimaryKey val id: String, val date: String, val raw: String, val scrollIndex: Int = 0)
@Entity(tableName = "reading", primaryKeys = ["editionId", "storyId"])
data class ReadState(val editionId: String, val storyId: String)
@Entity(tableName = "answers", primaryKeys = ["editionId", "questionId"])
data class Answer(val editionId: String, val questionId: String, val choice: Int)
@Entity(tableName = "notifications") data class Notified(@PrimaryKey val editionId: String)

@Dao interface WorldDao {
    @Query("SELECT * FROM editions ORDER BY date DESC") fun editions(): Flow<List<CachedEdition>>
    @Query("SELECT * FROM editions WHERE id = :id") suspend fun edition(id: String): CachedEdition?
    @Query("SELECT * FROM editions ORDER BY date DESC LIMIT 1") suspend fun latest(): CachedEdition?
    @Upsert suspend fun put(edition: CachedEdition)
    @Query("UPDATE editions SET scrollIndex = :position WHERE id = :id") suspend fun savePosition(id: String, position: Int)
    @Query("SELECT * FROM reading") fun readings(): Flow<List<ReadState>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mark(read: ReadState)
    @Query("DELETE FROM reading WHERE editionId = :edition AND storyId = :story") suspend fun unmark(edition: String, story: String)
    @Query("SELECT * FROM answers") fun answers(): Flow<List<Answer>>
    @Upsert suspend fun answer(answer: Answer)
    @Query("DELETE FROM answers WHERE editionId = :id") suspend fun retryQuiz(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun claimNotification(notified: Notified): Long
    @Query("DELETE FROM notifications WHERE editionId = :id") suspend fun undoClaim(id: String)
    @Query("DELETE FROM editions WHERE id NOT IN (SELECT id FROM editions ORDER BY date DESC LIMIT :keep)") suspend fun trimEditions(keep: Int)
    @Query("DELETE FROM reading WHERE editionId NOT IN (SELECT id FROM editions)") suspend fun trimReading()
    @Query("DELETE FROM answers WHERE editionId NOT IN (SELECT id FROM editions)") suspend fun trimAnswers()
    @Query("DELETE FROM editions") suspend fun clearEditions()
    @Query("DELETE FROM reading") suspend fun clearReading()
    @Query("DELETE FROM answers") suspend fun clearAnswers()
    // Notification marks deliberately survive cache clearing to avoid duplicate alerts.
    @Transaction suspend fun clearCache() { clearEditions(); clearReading(); clearAnswers() }
    @Transaction suspend fun trim(keep: Int) { trimEditions(keep); trimReading(); trimAnswers() }
}
@Database(entities = [CachedEdition::class, ReadState::class, Answer::class, Notified::class], version = 1, exportSchema = true)
abstract class WorldDatabase : RoomDatabase() { abstract fun worldDao(): WorldDao }
