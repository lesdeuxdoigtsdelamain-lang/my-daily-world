package uk.family.mydailyworld

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.settingsStore by preferencesDataStore("parent_settings")
data class Preferences(val loaded: Boolean = false, val error: Boolean = false, val pinSet: Boolean = false, val pages: String = BuildConfig.PAGES_URL,
    val workflow: String = BuildConfig.WORKFLOW_URL, val hour: Int = 17, val minute: Int = 0,
    val reminders: Boolean = true, val theme: String = "System", val font: Float = 1f,
    val readingLevel: String = "13–15", val keep: Int = 30, val hidden: Set<String> = emptySet(),
    val lastSync: String = "Never", val syncState: String = "Not checked", val lastPublication: String = "Unknown")

object PinHash {
    fun derive(pin: String, salt: ByteArray): ByteArray = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)).encoded
    fun matches(pin: String, salt: ByteArray, expected: ByteArray) = MessageDigest.isEqual(derive(pin,salt),expected)
}
class ParentPreferences(context: Context) {
    private val store = context.settingsStore
    private fun s(k: String) = stringPreferencesKey(k)
    val flow: Flow<Preferences> = store.data.catch { emit(preferencesOf(booleanPreferencesKey("unreadable") to true)) }.map { p -> Preferences(
        loaded = true, error = p[booleanPreferencesKey("unreadable")] ?: false,
        pinSet = p[s("pin_hash")] != null, pages = p[s("pages")] ?: BuildConfig.PAGES_URL,
        workflow = p[s("workflow")] ?: BuildConfig.WORKFLOW_URL,
        hour = p[intPreferencesKey("hour")] ?: 17, minute = p[intPreferencesKey("minute")] ?: 0,
        reminders = p[booleanPreferencesKey("reminders")] ?: true, theme = p[s("theme")] ?: "System",
        font = p[floatPreferencesKey("font")] ?: 1f, readingLevel = p[s("level")] ?: "13–15",
        keep = p[intPreferencesKey("keep")] ?: 30, hidden = p[stringSetPreferencesKey("hidden")] ?: emptySet(),
        lastSync = p[s("last_sync")] ?: "Never", syncState = p[s("sync_state")] ?: "Not checked",
        lastPublication = p[s("publication")] ?: "Unknown") }

    suspend fun setPin(pin: String) {
        require(pin.matches(Regex("[0-9]{4,8}")))
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = PinHash.derive(pin,salt)
        store.edit { p ->
            p[s("pin_salt")] = Base64.getEncoder().encodeToString(salt)
            p[s("pin_hash")] = Base64.getEncoder().encodeToString(hash)
            p[intPreferencesKey("attempts")] = 0; p[longPreferencesKey("lock_until")] = 0
        }
    }
    suspend fun verify(pin: String): Boolean {
        val p = store.data.first()
        if (System.currentTimeMillis() < (p[longPreferencesKey("lock_until")] ?: 0)) return false
        val valid = runCatching { PinHash.matches(pin,Base64.getDecoder().decode(p[s("pin_salt")]),
            Base64.getDecoder().decode(p[s("pin_hash")])) }.getOrDefault(false)
        store.edit { current ->
            val attempts = if (valid) 0 else (current[intPreferencesKey("attempts")] ?: 0) + 1
            current[intPreferencesKey("attempts")] = if (attempts >= 5) 0 else attempts
            if (attempts >= 5) current[longPreferencesKey("lock_until")] = System.currentTimeMillis()+60_000
        }
        return valid
    }
    suspend fun save(p: Preferences) = store.edit {
        it[s("pages")] = p.pages.trim().trimEnd('/') + if(p.pages.isBlank()) "" else "/"
        it[s("workflow")] = p.workflow.trim(); it[intPreferencesKey("hour")] = p.hour
        it[intPreferencesKey("minute")] = p.minute; it[booleanPreferencesKey("reminders")] = p.reminders
        it[s("theme")] = p.theme; it[floatPreferencesKey("font")] = p.font
        it[s("level")] = p.readingLevel; it[intPreferencesKey("keep")] = p.keep
        it[stringSetPreferencesKey("hidden")] = p.hidden
    }
    suspend fun syncStatus(status: String, publication: String? = null) = store.edit {
        it[s("last_sync")] = java.time.Instant.now().toString(); it[s("sync_state")] = status
        if (publication != null) it[s("publication")] = publication
    }
    suspend fun bundledEditionImported():Boolean = store.data.first()[booleanPreferencesKey("bundled_imported")] ?: false
    suspend fun markBundledImported() = store.edit {it[booleanPreferencesKey("bundled_imported")] = true}
}
