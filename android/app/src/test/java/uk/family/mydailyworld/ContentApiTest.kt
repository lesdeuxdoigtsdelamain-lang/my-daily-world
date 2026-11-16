package uk.family.mydailyworld

import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ContentApiTest {
    @get:Rule val temp=TemporaryFolder()
    private val clock=Clock.fixed(Instant.parse("2000-01-01T18:00:00Z"),ZoneOffset.UTC)
    private val edition get()=fixture().copy(sample=false,sampleLabel=null)
    private fun api(corrupt:Boolean=false,future:Boolean=false,missing:Boolean=false):ContentApi {
        val bytes=wire.encodeToString(edition).toByteArray()
        val m=Manifest(1,edition.id,if(future)"2099-01-01" else edition.date,
            "2000-01-01T17:35:00Z","2000-01-01T17:30:00Z","editions/2000-01-01.json",
            if(corrupt)"0".repeat(64) else contentHash(bytes),"success","none","compact")
        val client=OkHttpClient.Builder().addInterceptor {chain->
            val body=if(chain.request().url.encodedPath.endsWith("latest.json"))wire.encodeToString(m) else bytes.decodeToString()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if(missing)404 else 200).message("Offline fixture")
                .body(body.toResponseBody()).build()
        }.build()
        return ContentApi(temp.root,client)
    }
    @Test fun verifiedDatedJsonDownloadsThroughRealParser() {
        assertEquals(edition,api().fetch("https://family.github.io/news/",clock).first)
    }
    @Test fun corruptedHashCannotBeCached() {
        assertThrows(IllegalArgumentException::class.java){api(corrupt=true).fetch("https://family.github.io/news/",clock)}
    }
    @Test fun futureManifestRejectedBeforeEdition() {
        assertThrows(IllegalArgumentException::class.java){api(future=true).fetch("https://family.github.io/news/",clock)}
    }
    @Test fun missingManifestIsNotPretendedToBeConnectionFailure() {
        assertThrows(EditionNotPublished::class.java){api(missing=true).fetch("https://family.github.io/news/",clock)}
    }
}
