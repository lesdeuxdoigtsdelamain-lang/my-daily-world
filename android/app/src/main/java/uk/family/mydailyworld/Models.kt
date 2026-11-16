package uk.family.mydailyworld

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.security.MessageDigest
import java.time.*

val categories = listOf("World", "UK", "Science & Tech", "Environment", "People & Culture", "Money & Business", "Sport", "Muslim World")
val wire = Json { ignoreUnknownKeys = false; isLenient = false; coerceInputValues = false }
val london: ZoneId = ZoneId.of("Europe/London")
fun today(clock: Clock = Clock.systemUTC()): LocalDate = LocalDate.now(clock.withZone(london))

@Serializable data class SourceRef(val publisher: String, val url: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("retrieved_at") val retrievedAt: String,
    @SerialName("source_title") val sourceTitle: String,
    @SerialName("event_date") val eventDate: String? = null,
    val author: String? = null, val license: String? = null,
    @SerialName("license_url") val licenseUrl: String? = null)
@Serializable data class Fact(val id: String, val text: String, @SerialName("source_url") val sourceUrl: String)
@Serializable data class Word(val word: String, val definition: String)
@Serializable data class Story(val id: String, val category: String, val title: String, val summary: String,
    @SerialName("why_it_matters") val why: String,
    @SerialName("word_to_know") val word: Word? = null,
    val sensitivity: String = "low", val country: String? = null,
    @SerialName("source_refs") val sources: List<SourceRef>,
    @SerialName("supporting_fact_ids") val factIds: List<String>, val facts: List<Fact>,
    @SerialName("content_kind") val kind: String = "headline",
    @SerialName("age_suitability") val age: String = "10-15",
    @SerialName("uncertainty_notes") val uncertainty: List<String> = emptyList())
@Serializable data class HistorySource(val publisher: String, val url: String)
@Serializable data class History(@SerialName("month_day") val monthDay: String, val year: Int,
    val title: String, val summary: String, val sources: List<HistorySource>,
    @SerialName("verified_on") val verifiedOn: String)
@Serializable data class Question(val id: String, val question: String, val choices: List<String>,
    @SerialName("correct_index") val correct: Int, val explanation: String,
    @SerialName("story_id") val storyId: String, @SerialName("fact_id") val factId: String)
@Serializable data class Edition(@SerialName("schema_version") val schemaVersion: Int,
    @SerialName("edition_id") val id: String, @SerialName("edition_date") val date: String,
    val timezone: String, val language: String, @SerialName("generated_at") val generatedAt: String,
    val mode: String, @SerialName("ai_provider") val aiProvider: String,
    @SerialName("ai_status") val aiStatus: String,
    @SerialName("estimated_reading_minutes") val minutes: Int, val stories: List<Story>,
    @SerialName("on_this_day") val history: History? = null, val quiz: List<Question>,
    val sample: Boolean = false, @SerialName("sample_label") val sampleLabel: String? = null)
@Serializable data class Manifest(@SerialName("schema_version") val schemaVersion: Int,
    @SerialName("edition_id") val editionId: String, @SerialName("edition_date") val date: String,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("last_successful_generation") val lastSuccess: String,
    val url: String, val sha256: String, val state: String,
    @SerialName("ai_provider") val aiProvider: String, val mode: String)

fun publicHttps(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
        uri.port in listOf(-1, 443) && uri.host != "localhost" &&
        !uri.host.matches(Regex("[0-9.:]+")) && !uri.host.contains(':') && uri.host != "example.org"
}.getOrDefault(false)

fun validate(e: Edition, allowSample: Boolean = false): Edition {
    require(e.schemaVersion == 1 && e.timezone == "Europe/London" && e.language == "en-GB") { "Unsupported content schema" }
    val day = LocalDate.parse(e.date)
    require(e.id == "${day}-en-GB" && e.mode in listOf("compact", "full"))
    require(e.aiProvider in listOf("none", "cloudflare"))
    OffsetDateTime.parse(e.generatedAt)
    require(e.minutes in 1..20 && e.stories.size in 1..10 && e.quiz.size <= 5)
    require(!e.sample || (allowSample && e.sampleLabel == "SAMPLE — NOT CURRENT NEWS")) { "Sample is not live news" }
    require(e.stories.map { it.id }.distinct().size == e.stories.size)
    fun plain(s: String) = !s.contains('<') && !s.contains('>') && !s.contains('\u0000')
    e.stories.forEach { s ->
        require(s.category in categories && s.id.length in 8..80 && s.title.length in 5..250)
        require(s.summary.length <= 1800 && s.why.length <= 400 && s.age == "10-15")
        require(listOf(s.title, s.summary, s.why).all(::plain))
        require(s.sensitivity in listOf("low", "gentle") && s.kind in listOf("headline", "excerpt", "grounded_ai"))
        require(s.sources.isNotEmpty() && s.sources.size <= 4 && s.facts.isNotEmpty())
        s.sources.forEach { require(publicHttps(it.url)); OffsetDateTime.parse(it.publishedAt); OffsetDateTime.parse(it.retrievedAt) }
        require(s.factIds.isNotEmpty() && s.factIds.all { id -> s.facts.any { it.id == id } })
        require(s.facts.map { it.id }.distinct().size == s.facts.size)
        require(s.facts.all { f -> plain(f.text) && s.sources.any { it.url == f.sourceUrl } })
    }
    require(e.quiz.map { it.id }.distinct().size == e.quiz.size)
    e.quiz.forEach { q ->
        require(q.choices.size == 3 && q.choices.map { it.lowercase() }.distinct().size == 3 && q.correct in 0..2)
        val story = e.stories.single { it.id == q.storyId }
        require(q.factId in story.factIds && story.facts.single { it.id == q.factId }.text.contains(q.choices[q.correct]))
    }
    e.history?.let { h ->
        require(h.monthDay == e.date.substring(5) && h.year in 1583 until day.year)
        require(h.sources.isNotEmpty() && h.sources.all { publicHttps(it.url) })
        require(plain(h.title) && plain(h.summary)); LocalDate.parse(h.verifiedOn)
    }
    if (e.mode == "full") require(e.stories.size >= 7 && e.quiz.size == 5 &&
        e.stories.all { it.summary.split(Regex("\\s+")).size in 80..115 })
    return e
}
fun contentHash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
fun parseEdition(raw: String, allowSample: Boolean = false): Edition = validate(wire.decodeFromString<Edition>(raw), allowSample)

fun nextReminder(now: Instant, hour: Int = 17, minute: Int = 0): Instant {
    val local = now.atZone(london)
    var target = local.toLocalDate().atTime(hour, minute).atZone(london)
    if (!target.toInstant().isAfter(now)) target = target.plusDays(1)
    return target.toInstant()
}
fun shouldNotify(e: Edition, now: Instant, hour: Int, minute: Int): Boolean =
    !e.sample && e.date == now.atZone(london).toLocalDate().toString() &&
        !now.atZone(london).toLocalTime().isBefore(LocalTime.of(hour, minute))
