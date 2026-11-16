package uk.family.mydailyworld

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],application=Application::class)
class RepositoryTest {
    private lateinit var db:WorldDatabase
    private lateinit var prefs:ParentPreferences
    @Before fun setup() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        db=Room.inMemoryDatabaseBuilder(context,WorldDatabase::class.java).allowMainThreadQueries().build()
        prefs=ParentPreferences(context)
    }
    @After fun close(){db.close()}
    @Test fun downloadCacheProgressQuizAndOfflineRetained() = runBlocking {
        val day="2026-11-16"
        val e=fixture().copy(sample=false,sampleLabel=null,date=day,id="$day-en-GB")
        var offline=false
        val source=object:ContentSource {
            override fun fetch(base:String,clock:Clock):Pair<Edition,String> {
                if(offline)throw IOException("offline")
                return e to "2026-11-16T17:35:00Z"
            }
        }
        prefs.save(Preferences(pages="https://family.github.io/news/"))
        val repo=NewsRepository(db.worldDao(),prefs,source)
        val clock=Clock.fixed(Instant.parse("2026-11-16T17:40:00Z"),ZoneOffset.UTC)
        assertTrue(repo.sync(clock).ready)
        db.worldDao().mark(ReadState(e.id,e.stories.first().id))
        db.worldDao().answer(Answer(e.id,e.quiz.first().id,1))
        db.worldDao().savePosition(e.id,2)
        offline=true
        assertTrue(repo.sync(clock).failed)
        assertEquals(e.id,repo.latestValidated()?.id)
        assertEquals(2,db.worldDao().edition(e.id)?.scrollIndex)
        assertEquals(1,db.worldDao().readings().first().size)
        assertEquals(1,db.worldDao().answers().first().size)
        // A fresh repository still reads the same local state.
        assertEquals(e,NewsRepository(db.worldDao(),prefs,source).latestValidated())
    }
    @Test fun notificationClaimIsAtomicAndSurvivesCacheClear() = runBlocking {
        val dao=db.worldDao()
        val claims=(1..12).map {async(Dispatchers.Default){dao.claimNotification(Notified("2026-11-16-en-GB"))}}.awaitAll()
        assertEquals(1,claims.count {it!=-1L})
        dao.clearCache()
        assertEquals(-1L,dao.claimNotification(Notified("2026-11-16-en-GB")))
    }
    @Test fun staleEditionIsNotLabelledToday() = runBlocking {
        val e=fixture().copy(sample=false,sampleLabel=null)
        prefs.save(Preferences(pages="https://family.github.io/news/"))
        val repo=NewsRepository(db.worldDao(),prefs,object:ContentSource {
            override fun fetch(base:String,clock:Clock)=e to "2000-01-01T17:00:00Z"
        })
        val result=repo.sync(Clock.fixed(Instant.parse("2026-11-16T17:30:00Z"),ZoneOffset.UTC))
        assertFalse(result.ready);assertEquals("Not published yet",result.message)
    }
}
