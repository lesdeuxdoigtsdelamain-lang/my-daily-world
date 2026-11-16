package uk.family.mydailyworld

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class EditionNotPublished: IOException()

interface ContentSource { fun fetch(base: String, clock: Clock = Clock.systemUTC()): Pair<Edition, String> }
class ContentApi(cache: File, private val client: OkHttpClient = OkHttpClient.Builder()
    .cache(Cache(cache, 5L * 1024 * 1024)).connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20,TimeUnit.SECONDS).callTimeout(35, TimeUnit.SECONDS)
    .followRedirects(false).build()) : ContentSource {
    fun get(url: String, max: Int): ByteArray {
        require(publicHttps(url)) { "Use a public HTTPS address" }
        val request = Request.Builder().url(url).cacheControl(CacheControl.FORCE_NETWORK)
            .header("User-Agent", "MyDailyWorld/1.0").build()
        client.newCall(request).execute().use { response ->
            if(response.code==404)throw EditionNotPublished()
            if (!response.isSuccessful) throw IOException("Server returned HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty server response")
            if (body.contentLength() > max) throw IOException("Content is too large")
            val bytes = body.byteStream().use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while(out.size()<=max) {
                    val n=input.read(buffer,0,minOf(buffer.size,max+1-out.size()))
                    if(n<0)break
                    out.write(buffer,0,n)
                }
                out.toByteArray()
            }
            if (bytes.size > max) throw IOException("Content is too large")
            return bytes
        }
    }
    override fun fetch(base: String, clock: Clock): Pair<Edition, String> {
        val root = base.trimEnd('/') + "/"
        val manifest = wire.decodeFromString<Manifest>(get(root+"latest.json",64_000).decodeToString())
        require(manifest.schemaVersion == 1 && manifest.state == "success") { "Unsupported manifest schema" }
        require(manifest.url.matches(Regex("editions/\\d{4}-\\d{2}-\\d{2}\\.json"))) { "Invalid edition address" }
        require(!LocalDate.parse(manifest.date).isAfter(today(clock))) { "Future edition rejected" }
        OffsetDateTime.parse(manifest.publishedAt); OffsetDateTime.parse(manifest.lastSuccess)
        val bytes = get(root+manifest.url, 512_000)
        require(contentHash(bytes) == manifest.sha256) { "Content integrity check failed" }
        val edition = parseEdition(bytes.decodeToString())
        require(edition.id == manifest.editionId && edition.date == manifest.date && edition.mode == manifest.mode)
        return edition to manifest.publishedAt
    }
}

data class SyncResult(val message: String, val ready: Boolean = false, val failed: Boolean = false)
class NewsRepository(val dao: WorldDao, val preferences: ParentPreferences, private val api: ContentSource) {
    private val mutex = Mutex()
    suspend fun sync(clock: Clock = Clock.systemUTC()): SyncResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val settings = preferences.flow.first()
            if (settings.pages.isBlank()) return@withContext SyncResult("Ask a parent to connect the edition address in Parent Settings.")
            val result = try {
                val (e, published) = api.fetch(settings.pages, clock)
                val old = dao.edition(e.id)
                dao.put(CachedEdition(e.id,e.date,wire.encodeToString(e),old?.scrollIndex ?: 0))
                dao.trim(settings.keep)
                preferences.syncStatus(if(e.date==today(clock).toString()) "Today's edition is ready" else "Not published yet",published)
                SyncResult(if(e.date==today(clock).toString()) "Today's edition is ready" else "Not published yet",e.date==today(clock).toString())
            } catch (_: EditionNotPublished) {
                preferences.syncStatus("Not published yet")
                SyncResult("Not published yet")
            } catch (_: IOException) {
                SyncResult("Couldn't connect. Downloaded editions are available offline.",failed=true)
            } catch (_: Exception) {
                SyncResult("The edition could not be verified. Please try again later.",failed=true)
            }
            if(result.failed) preferences.syncStatus(result.message)
            result
        }
    }
    suspend fun latestValidated(): Edition? = withContext(Dispatchers.IO) {
        dao.latest()?.let { runCatching { parseEdition(it.raw) }.getOrNull() }
    }
}
