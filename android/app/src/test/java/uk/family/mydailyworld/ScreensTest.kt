package uk.family.mydailyworld

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Native Compose semantics under Robolectric; separate from real-device tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33],application=Application::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ScreensTest {
    @get:Rule val ui=createComposeRule()
    @Test fun datedCardsHistoryQuizAndArchiveUseRealState() {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val e=parseEdition(context.assets.open("initial-edition.json").bufferedReader().readText())
        var page by mutableStateOf(0)
        var read by mutableStateOf(emptySet<String>())
        var answers by mutableStateOf(emptyList<Answer>())
        ui.setContent {
            MaterialTheme {
                when(page) {
                    0->EditionScreen(e,e.stories,0,read,false,"13–15",{}, {},
                        {id,value->read=if(value)read+id else read-id},{})
                    1->HistoryScreen(e,{})
                    2->QuizScreen(e,answers,{q,c->answers=answers+Answer(e.id,q,c)},{answers=emptyList()})
                    else->ArchiveScreen(listOf(e),read.map {ReadState(e.id,it)},answers,{})
                }
            }
        }
        ui.onAllNodesWithText("Mark as read").onFirst().performScrollTo().performClick()
        ui.onNodeWithText("Read — mark as unread").assertExists()
        ui.runOnIdle {page=1}
        ui.onNodeWithText(e.history!!.title).assertExists()
        ui.runOnIdle {page=2}
        val q=e.quiz.first()
        ui.onAllNodesWithText(q.choices[q.correct]).onFirst().performScrollTo().performClick()
        ui.onNodeWithText("That's right!").assertExists()
        ui.onNodeWithText(q.explanation).assertExists()
        ui.runOnIdle {page=3}
        ui.onNodeWithText("Your reading shelf").assertExists()
        ui.onNodeWithText("1 read",substring=true).assertExists()
    }
}
