package uk.family.mydailyworld

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun displayDate(value:String):String = runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",Locale.UK)) }.getOrDefault(value)
fun displayTime(value:String):String = runCatching {OffsetDateTime.parse(value).atZoneSameInstant(london)
    .format(DateTimeFormatter.ofPattern("d MMM, HH:mm 'UK'",Locale.UK))}.getOrDefault(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WorldApp(vm:WorldViewModel,openLink:(String)->Unit,askNotifications:()->Unit) {
    val p by vm.settings.collectAsStateWithLifecycle()
    if(!p.loaded) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
        return
    }
    if(p.error) {
        Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) {
            Notice("Settings could not be read safely. Ask a parent to restart the app. If this continues, reset app storage in Android settings.")
        }
        return
    }
    val cached by vm.cached.collectAsStateWithLifecycle()
    val reads by vm.read.collectAsStateWithLifecycle()
    val answers by vm.answers.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    var page by rememberSaveable {mutableStateOf("Today")}
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    val entries=remember(cached) {cached.mapNotNull {c->runCatching {parseEdition(c.raw,BuildConfig.DEBUG) to c}.getOrNull()}}
    val current=entries.firstOrNull {it.first.id==selected} ?: entries.firstOrNull()
    val edition=current?.first
    val tabs=listOf("Today","Categories","On This Day","Quiz","Archive")
    val icons=listOf(Icons.Default.Public,Icons.Default.GridView,Icons.Default.History,Icons.AutoMirrored.Filled.HelpOutline,Icons.Default.Inventory2)
    var pendingLink by remember {mutableStateOf<String?>(null)}
    val link:(String)->Unit={pendingLink=it}
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rail=maxWidth>=760.dp
        Row {
            if(rail) NavigationRail {
                Spacer(Modifier.height(32.dp))
                tabs.forEachIndexed {i,t->NavigationRailItem(selected=page==t,onClick={page=t},icon={Icon(icons[i],t)},label={Text(t)})}
            }
            Scaffold(modifier=Modifier.weight(1f),topBar={TopAppBar(title={Text("My Daily World",fontWeight=FontWeight.Bold)},actions={
                IconButton(onClick={page="Parent Settings"}){Icon(Icons.Default.Lock,"Parent Settings")}
            })},bottomBar={if(!rail) NavigationBar {
                tabs.forEachIndexed {i,t->NavigationBarItem(selected=page==t,onClick={page=t},icon={Icon(icons[i],t)},
                    label={Text(if(t=="On This Day")"History" else t,maxLines=1)})}
            }}) {padding->
                Column(Modifier.padding(padding).fillMaxSize().widthIn(max=1000.dp).padding(horizontal=16.dp)) {
                    if(edition?.sample==true) Notice("SAMPLE — NOT CURRENT NEWS")
                    if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(vertical=8.dp))
                    if(cached.isNotEmpty() && entries.isEmpty())Notice("Downloaded content could not be read. Ask a parent to clear the cache and check again.")
                    when(page) {
                        "Parent Settings" -> if(unlocked) ParentScreen(vm,p,openLink,askNotifications) else PinScreen(p.pinSet,vm)
                        "Archive" -> ArchiveScreen(entries.map {it.first},reads,answers){selected=it;page="Today"}
                        "On This Day" -> HistoryScreen(edition,link)
                        "Quiz" -> QuizScreen(edition,answers,onAnswer={q,c->edition?.let {vm.answer(it.id,q,c)}},onRetry={edition?.let {vm.retry(it.id)}})
                        else -> if(edition==null) {
                            Text("A small window on a wide world",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(vertical=20.dp))
                            Notice("Today's edition isn't ready yet")
                            Text("Ask a parent to connect the news address in Parent Settings. Downloaded editions will stay here for offline reading.",modifier=Modifier.padding(vertical=16.dp))
                            Button(onClick={vm.sync()},enabled=!checking){Text("Check for new edition")}
                        } else {
                            var filter by rememberSaveable {mutableStateOf("All")}
                            if(page=="Categories") {
                                Text("Explore the sections",style=MaterialTheme.typography.headlineSmall)
                                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    items(listOf("All")+categories){c->FilterChip(selected=filter==c,onClick={filter=c},label={Text(c)})}
                                }
                            }
                            val stories=edition.stories.filter {s-> if(page=="Categories")filter=="All" || s.category==filter else s.category !in p.hidden}
                            key(edition.id,page,filter) {
                                EditionScreen(edition,stories,current?.second?.scrollIndex ?: 0,reads.filter {it.editionId==edition.id && edition.stories.any {s->s.id==it.storyId}}.map {it.storyId}.toSet(),
                                    checking,p.readingLevel,link,{vm.sync()},{s,r->vm.mark(edition.id,s,r)},
                                    {index->if(page=="Today")vm.position(edition.id,index)})
                            }
                        }
                    }
                }
            }
        }
        // First launch requires a parent-chosen PIN. No default PIN ships in the APK.
        if(!p.pinSet) AlertDialog(onDismissRequest={},title={Text("Welcome to My Daily World")},text={
            Column {Text("A parent sets up this app. Create a local PIN to protect Parent Settings.")
                PinScreen(false,vm)}
        },confirmButton={})
    }
    pendingLink?.let {url-> AlertDialog(onDismissRequest={pendingLink=null},title={Text("Open the original source?")},
        text={Text("This opens an external website in your browser. Its content follows the publisher's rules.")},
        confirmButton={TextButton(onClick={pendingLink=null;openLink(url)}){Text("Open browser")}},
        dismissButton={TextButton(onClick={pendingLink=null}){Text("Stay here")}}) }
}

@Composable fun Notice(text:String) {Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.medium,
    modifier=Modifier.fillMaxWidth().padding(vertical=8.dp)){Text(text,modifier=Modifier.padding(16.dp))}}

@Composable fun EditionScreen(e:Edition,stories:List<Story>,position:Int,read:Set<String>,checking:Boolean,level:String,
    link:(String)->Unit,refresh:()->Unit,mark:(String,Boolean)->Unit,savePosition:(Int)->Unit) {
    val state=rememberLazyListState(initialFirstVisibleItemIndex=position.coerceIn(0,stories.size))
    LaunchedEffect(state) {snapshotFlow {state.firstVisibleItemIndex}.distinctUntilChanged().collect {savePosition(it)}}
    LazyColumn(state=state,verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item {
            Text(if(e.sample)"Sample edition" else if(e.date==today().toString())"Today's edition" else "Last downloaded edition",style=MaterialTheme.typography.headlineMedium)
            Text(displayDate(e.date),style=MaterialTheme.typography.titleMedium)
            if(e.date!=today().toString() && !e.sample)Notice("Today's edition isn't ready yet. You are reading an older edition.")
            Text("About ${e.minutes} min · ${read.size} of ${e.stories.size} stories read",modifier=Modifier.padding(vertical=8.dp))
            LinearProgressIndicator(progress={read.size.toFloat()/e.stories.size},modifier=Modifier.fillMaxWidth())
            if(e.mode=="compact") Notice("Compact edition — a shorter read with the sources available today.")
            OutlinedButton(onClick=refresh,enabled=!checking){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Check for new edition")}
            if(stories.isEmpty())Text("No stories in this section today. Try another section.")
        }
        items(stories,key={it.id}) {s->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Icon(if(s.category=="Science & Tech")Icons.Default.Science else if(s.category=="Environment")Icons.Default.Eco else Icons.Default.Public,null)
                        Text(s.category,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    }
                    s.sources.filter {it.author!=null && it.license!=null}.forEach {credit->
                        Text("Originally published on ${credit.publisher} by ${credit.author}",style=MaterialTheme.typography.bodySmall)
                        TextButton(onClick={link(credit.url)}){Text("Original story and author")}
                    }
                    Text(s.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
                    s.country?.let {Text(it)}
                    if(s.kind=="headline") Text("Headline from ${s.sources.first().publisher}. Open the original article for the full report.")
                    if(s.summary.isNotBlank())Text("From ${s.sources.first().publisher}'s report",style=MaterialTheme.typography.labelLarge)
                    if(s.summary.isNotBlank())Text(s.summary,style=if(level=="10–12") MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
                    Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.small) {
                        Column(Modifier.padding(12.dp)) {Text("Why it matters",fontWeight=FontWeight.Bold);Text(s.why)}
                    }
                    s.word?.let {Text("Word to know: ${it.word} — ${it.definition}")}
                    s.uncertainty.forEach {Text(it,style=MaterialTheme.typography.bodySmall)}
                    s.sources.forEach {source->
                        Text("${source.publisher} · Published ${displayTime(source.publishedAt)}",style=MaterialTheme.typography.labelLarge)
                        source.author?.let {Text("By $it",style=MaterialTheme.typography.bodySmall)}
                        source.license?.let {licence->
                            Text("Short extract · $licence · no press images",style=MaterialTheme.typography.bodySmall)
                            source.licenseUrl?.let {url->TextButton(onClick={link(url)}){Text("Content licence")}}
                        }
                        Text("Retrieved ${displayTime(source.retrievedAt)}",style=MaterialTheme.typography.bodySmall)
                        source.eventDate?.let {Text("Event date: $it",style=MaterialTheme.typography.bodySmall)}
                        TextButton(onClick={link(source.url)}){Icon(Icons.AutoMirrored.Filled.OpenInNew,null);Spacer(Modifier.width(8.dp));Text("Read original article")}
                    }
                    OutlinedButton(onClick={mark(s.id,s.id !in read)}){
                        Icon(if(s.id in read)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null)
                        Spacer(Modifier.width(8.dp));Text(if(s.id in read)"Read — mark as unread" else "Mark as read")
                    }
                }
            }
        }
    }
}

@Composable fun HistoryScreen(e:Edition?,link:(String)->Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("On This Day",style=MaterialTheme.typography.headlineMedium)
        Text(e?.date?.let(::displayDate) ?: displayDate(today().toString()))
        val h=e?.history
        if(h==null) Notice("No verified history highlight today") else {
            Text(h.year.toString(),style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.primary)
            Text(h.title,style=MaterialTheme.typography.headlineSmall);Text(h.summary)
            h.sources.forEach {s->OutlinedButton(onClick={link(s.url)}){Text("Reference: ${s.publisher}")}}
        }
    }
}

@Composable fun QuizScreen(e:Edition?,answers:List<Answer>,onAnswer:(String,Int)->Unit,onRetry:()->Unit) {
    val questions=e?.quiz.orEmpty()
    val saved=answers.filter {it.editionId==e?.id}.associate {it.questionId to it.choice}
    LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item {
            Text("Daily Quiz",style=MaterialTheme.typography.headlineMedium)
            e?.let {Text(displayDate(it.date))}
            if(questions.isEmpty()) Notice("No verified quiz questions are available for this edition.")
            else Text("${questions.size} questions based on this edition. Take your time.")
            if(questions.size in 1..4)Text("A shorter quiz today — we only ask questions supported by the stories.")
        }
        items(questions,key={it.id}) {q->
            val choice=saved[q.id]
            ElevatedCard {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(q.question,style=MaterialTheme.typography.titleMedium)
                    q.choices.forEachIndexed {i,c->OutlinedButton(onClick={onAnswer(q.id,i)},enabled=choice==null,modifier=Modifier.fillMaxWidth()) {Text(c)}}
                    if(choice!=null) {
                        Text(if(choice==q.correct)"That's right!" else "Good try. The answer is ${q.choices[q.correct]}.",fontWeight=FontWeight.Bold)
                        Text(q.explanation)
                    }
                }
            }
        }
        if(questions.isNotEmpty() && questions.all {it.id in saved}) item {
            val score=questions.count {saved[it.id]==it.correct}
            Notice("Great work! You understood $score out of ${questions.size}.")
            Button(onClick=onRetry){Text("Try again")}
        }
    }
}

@Composable fun ArchiveScreen(editions:List<Edition>,reads:List<ReadState>,answers:List<Answer>,open:(String)->Unit) {
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item {Text("Your reading shelf",style=MaterialTheme.typography.headlineMedium)
            Text("Downloaded editions · available offline",modifier=Modifier.padding(vertical=8.dp))
            if(editions.isEmpty())Notice("Your downloaded editions will appear here.")}
        items(editions,key={it.id}) {e->
            ElevatedCard(onClick={open(e.id)}) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(displayDate(e.date),style=MaterialTheme.typography.titleMedium)
                    Text("${e.stories.size} stories · ${e.stories.flatMap {it.sources}.map {it.publisher}.distinct().size} sources · Offline")
                    Text("${reads.count {it.editionId==e.id}} read · ${if(e.date==today().toString())"Today's edition" else "Past edition"}")
                    val saved=answers.filter {it.editionId==e.id}.associate {it.questionId to it.choice}
                    if(e.quiz.isNotEmpty() && e.quiz.all {it.id in saved})Text("Quiz: ${e.quiz.count {saved[it.id]==it.correct}} out of ${e.quiz.size}")
                    if(e.sample)Text("SAMPLE — NOT CURRENT NEWS",color=MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
