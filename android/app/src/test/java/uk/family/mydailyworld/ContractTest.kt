package uk.family.mydailyworld

import org.junit.Assert.*
import org.junit.Test
import java.time.*

fun fixture():Edition = parseEdition(object {}.javaClass.getResource("/sample.json")!!.readText(),allowSample=true)

class ContractTest {
    @Test fun sampleRequiresExplicitOptIn() {
        val e=fixture();assertEquals(5,e.quiz.size)
        assertThrows(IllegalArgumentException::class.java){validate(e)}
    }
    @Test fun rejectsSchemaAndUncitedQuiz() {
        val e=fixture()
        assertThrows(IllegalArgumentException::class.java){validate(e.copy(schemaVersion=99),true)}
        assertThrows(IllegalArgumentException::class.java){validate(e.copy(quiz=listOf(e.quiz.first().copy(factId="invented"))),true)}
    }
    @Test fun rejectsHttpAndMarkup() {
        assertFalse(publicHttps("http://news.example.com"))
        assertFalse(publicHttps("https://user:secret@github.com/"))
        val e=fixture()
        assertThrows(IllegalArgumentException::class.java){validate(e.copy(stories=listOf(e.stories.first().copy(title="<script>bad</script>"))),true)}
    }
    @Test fun contentDigestStable() {assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",contentHash("abc".toByteArray()))}
    @Test fun reminderTracksUkSummerAndWinter() {
        assertEquals(Instant.parse("2026-07-16T16:00:00Z"),nextReminder(Instant.parse("2026-07-16T10:00:00Z")))
        assertEquals(Instant.parse("2026-11-16T17:00:00Z"),nextReminder(Instant.parse("2026-11-16T10:00:00Z")))
        assertEquals(Instant.parse("2026-03-29T16:00:00Z"),nextReminder(Instant.parse("2026-03-28T18:00:00Z")))
        assertEquals(Instant.parse("2026-10-25T17:00:00Z"),nextReminder(Instant.parse("2026-10-24T18:00:00Z")))
    }
    @Test fun dateUsesLondonRatherThanTabletTimeZone() {
        val c=Clock.fixed(Instant.parse("2026-07-16T23:30:00Z"),ZoneId.of("America/New_York"))
        assertEquals(LocalDate.parse("2026-07-17"),today(c))
    }
    @Test fun neverNotifiesSampleStaleOrBeforeReminder() {
        val e=fixture().copy(sample=false,sampleLabel=null,date="2026-11-16",id="2026-11-16-en-GB")
        assertFalse(shouldNotify(e,Instant.parse("2026-11-16T16:59:59Z"),17,0))
        assertTrue(shouldNotify(e,Instant.parse("2026-11-16T17:30:00Z"),17,0))
        assertFalse(shouldNotify(e,Instant.parse("2026-11-17T17:00:00Z"),17,0))
        assertFalse(shouldNotify(e.copy(sample=true),Instant.parse("2026-11-16T17:30:00Z"),17,0))
    }
    @Test fun saltedPinIsNotPlaintextOrConstant() {
        val a=ByteArray(16){1};val b=ByteArray(16){2}
        assertFalse(PinHash.derive("1234",a).contentEquals(PinHash.derive("1234",b)))
        assertTrue(PinHash.matches("1234",a,PinHash.derive("1234",a)))
        assertFalse(PinHash.matches("9876",a,PinHash.derive("1234",a)))
    }
}
