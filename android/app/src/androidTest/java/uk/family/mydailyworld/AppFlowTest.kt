package uk.family.mydailyworld

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

/** Real native Compose tests. No GitHub login, external browser or AI is simulated as successful. */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val ui=createEmptyComposeRule()
    private lateinit var scenario:ActivityScenario<MainActivity>
    @Before fun launch() {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.world
        runBlocking {
            app.preferences.setPin("4729") // test APK only; production always asks the parent
            app.preferences.save(Preferences(pages="",workflow=""))
            app.database.worldDao().clearCache()
            val e=parseEdition(app.assets.open("initial-edition.json").bufferedReader().readText())
            app.database.worldDao().put(CachedEdition(e.id,e.date,wire.encodeToString(e)))
        }
        scenario=ActivityScenario.launch(MainActivity::class.java)
        ui.waitUntil(30_000){ui.onAllNodesWithText("Today's edition",substring=true).fetchSemanticsNodes().isNotEmpty() ||
            ui.onAllNodesWithText("Last downloaded edition").fetchSemanticsNodes().isNotEmpty()}
    }
    @After fun close(){scenario.close()}
    @Test fun sixScreensAndPinProtectedGeneration() {
        ui.onNodeWithContentDescription("Categories",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Explore the sections").assertExists()
        ui.onNodeWithContentDescription("On This Day",useUnmergedTree=true).performClick()
        ui.onNodeWithText("UNESCO is founded").assertExists()
        ui.onNodeWithContentDescription("Quiz",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Daily Quiz").assertExists()
        ui.onNodeWithContentDescription("Archive",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Your reading shelf").assertExists()
        ui.onNodeWithContentDescription("Parent Settings").performClick()
        ui.onNodeWithText("Generate Today's News").assertDoesNotExist()
        ui.onNodeWithText("Parent PIN").performTextInput("4729")
        ui.onNodeWithText("Unlock",useUnmergedTree=true).performClick()
        ui.waitUntil(20_000){ui.onAllNodesWithText("Generate Today's News").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Generate Today's News").assertIsNotEnabled()
        // No workflow URL: the app must not pretend to have triggered a job.
    }
    @Test fun readingProgressSurvivesActivityRecreation() {
        ui.onAllNodesWithText("Mark as read").onFirst().performScrollTo().performClick()
        scenario.recreate()
        ui.waitUntil(30_000){ui.onAllNodesWithText("1 of",substring=true).fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("1 of",substring=true).assertExists()
    }
}
