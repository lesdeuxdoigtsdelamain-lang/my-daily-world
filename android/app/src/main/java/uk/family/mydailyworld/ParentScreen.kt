package uk.family.mydailyworld

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable fun PinScreen(existing:Boolean,vm:WorldViewModel) {
    var pin by remember {mutableStateOf("")}
    var confirm by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf("")}
    Column(Modifier.padding(vertical=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(existing)"Parent Settings" else "Create a parent PIN",style=MaterialTheme.typography.titleLarge)
        Text(if(existing)"Enter your parent PIN. After five incorrect attempts, wait one minute." else "Choose 4–8 digits. Keep your PIN somewhere safe.")
        OutlinedTextField(value=pin,onValueChange={pin=it.filter(Char::isDigit).take(8)},label={Text("Parent PIN")},
            visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
        if(!existing)OutlinedTextField(value=confirm,onValueChange={confirm=it.filter(Char::isDigit).take(8)},label={Text("Repeat PIN")},
            visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true)
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        Button(onClick={
            busy=true;error=""
            vm.pin(pin,!existing){ok->busy=false;if(!ok){error="PIN not accepted. If you tried five times, wait one minute.";pin=""}}
        },enabled=pin.length>=4 && (existing || pin==confirm) && !busy){Text(if(busy)"Checking…" else if(existing)"Unlock" else "Save parent PIN")}
        if(existing)Text("Forgotten PIN? A parent can reset app storage in Android settings. This also removes downloaded editions, scores and settings.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable fun ParentScreen(vm:WorldViewModel,saved:Preferences,openLink:(String)->Unit,askNotifications:()->Unit) {
    val context=LocalContext.current
    var draft by remember {mutableStateOf(saved)}
    var hour by remember(saved.hour){mutableStateOf(saved.hour.toString().padStart(2,'0'))}
    var minute by remember(saved.minute){mutableStateOf(saved.minute.toString().padStart(2,'0'))}
    var generate by remember {mutableStateOf(false)}
    var clear by remember {mutableStateOf(false)}
    var newPin by remember {mutableStateOf("")}
    var repeatPin by remember {mutableStateOf("")}
    var error by remember {mutableStateOf("")}
    val checking by vm.checking.collectAsStateWithLifecycle()
    val cached by vm.cached.collectAsStateWithLifecycle()
    val latest=remember(cached){cached.firstOrNull()?.let {runCatching {parseEdition(it.raw,BuildConfig.DEBUG)}.getOrNull()}}
    val workflowOkay=draft.workflow.matches(Regex("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/actions/workflows/publish-daily\\.yml"))
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Parent Settings",style=MaterialTheme.typography.headlineMedium)
        Text("Settings, reading progress and quiz answers stay on this device. No child account is needed.")
        OutlinedButton(onClick={vm.lock()}){Text("Lock Parent Settings")}
        HorizontalDivider()
        Text("Connect your editions",style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(value=draft.pages,onValueChange={draft=draft.copy(pages=it)},label={Text("GitHub Pages base address")},
            placeholder={Text("https://your-account.github.io/my-daily-world/")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        OutlinedTextField(value=draft.workflow,onValueChange={draft=draft.copy(workflow=it)},label={Text("Publish Daily workflow address")},
            modifier=Modifier.fillMaxWidth(),singleLine=true)
        Text("Use the repository's public Pages address. Never paste API tokens or passwords here.",style=MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick={
            if(draft.pages.isBlank() || !publicHttps(draft.pages) || java.net.URI(draft.pages).query!=null || java.net.URI(draft.pages).fragment!=null)error="Enter a public HTTPS Pages address without tokens."
            else if(!workflowOkay)error="Enter the repository's Publish Daily workflow address."
            else {error="";vm.save(saved.copy(pages=draft.pages,workflow=draft.workflow))}
        }){Text("Save connection")}
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        val connectedWorkflow=workflowOkay && draft.workflow.trim()==saved.workflow.trim()
        Button(onClick={generate=true},enabled=connectedWorkflow,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text("Generate Today's News")}
        if(!connectedWorkflow)Text("Save a valid GitHub Publish Daily workflow address to enable generation.")
        OutlinedButton(onClick={vm.sync()},enabled=!checking,modifier=Modifier.fillMaxWidth()) {Text(if(checking)"Checking…" else "Check for Today's Edition")}
        Text("Opening GitHub does not start a workflow. The signed-in parent must press Run workflow there.")
        HorizontalDivider()
        Text("Daily reminder · Europe/London",style=MaterialTheme.typography.titleLarge)
        Row {Text("Daily, including weekends",Modifier.weight(1f));Switch(checked=draft.reminders,onCheckedChange={draft=draft.copy(reminders=it)})}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(hour,{hour=it.filter(Char::isDigit).take(2)},label={Text("Hour (0–23)")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            OutlinedTextField(minute,{minute=it.filter(Char::isDigit).take(2)},label={Text("Minute")},modifier=Modifier.weight(1f),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
        }
        Text("UK daylight saving is handled automatically. Android may delay reminders; they only announce a downloaded edition for today.")
        Text(if(Notifier.allowed(context))"Notifications are allowed" else "Notifications are off")
        OutlinedButton(onClick=askNotifications){Text("Allow notifications")}
        OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))}){Text("Android notification settings")}
        OutlinedButton(onClick={vm.message.value=if(Notifier.test(context))"Test notification sent" else "Notifications are blocked in Android settings"}){Text("Test notification")}
        HorizontalDivider()
        Text("Reading",style=MaterialTheme.typography.titleLarge)
        Text("Appearance")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("System","Light","Dark").forEach {t->FilterChip(selected=draft.theme==t,onClick={draft=draft.copy(theme=t)},label={Text(t)})}}
        Text("Text size: ${(draft.font*100).toInt()}%")
        Slider(value=draft.font,onValueChange={draft=draft.copy(font=it)},valueRange=1f..1.5f,steps=4)
        Text("Reading display · both levels use the same verified stories")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("10–12","13–15").forEach {level->FilterChip(selected=draft.readingLevel==level,onClick={draft=draft.copy(readingLevel=level)},label={Text(level)})}}
        Text("Sections on Today · all sections remain available under Categories")
        categories.forEach {category->
            Row {Text(category,Modifier.weight(1f).padding(top=12.dp));Switch(checked=category !in draft.hidden,onCheckedChange={checked->
                draft=draft.copy(hidden=if(checked)draft.hidden-category else draft.hidden+category)})}
        }
        Text("Downloaded editions to keep: ${draft.keep}")
        Slider(value=draft.keep.toFloat(),onValueChange={draft=draft.copy(keep=it.toInt())},valueRange=30f..90f,steps=5)
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
        Button(onClick={
            val h=hour.toIntOrNull();val m=minute.toIntOrNull()
            if(h==null || h !in 0..23 || m==null || m !in 0..59)error="Enter a valid reminder time."
            else if(draft.pages.isNotBlank() && (!publicHttps(draft.pages) || java.net.URI(draft.pages).query!=null || java.net.URI(draft.pages).fragment!=null))error="Use a public HTTPS Pages address without tokens or query parameters."
            else if(draft.workflow.isNotBlank() && !workflowOkay)error="Use https://github.com/OWNER/REPO/actions/workflows/publish-daily.yml"
            else {error="";vm.save(draft.copy(hour=h,minute=m))}
        },modifier=Modifier.fillMaxWidth()){Text("Save settings")}
        HorizontalDivider()
        Text("Publication and sync",style=MaterialTheme.typography.titleLarge)
        Text("Last check: ${saved.lastSync}\nStatus: ${saved.syncState}\nLast publication: ${saved.lastPublication}")
        latest?.let {Text("Downloaded: ${it.date}\nMode: ${it.mode}\nAI: ${it.aiProvider}\n${it.aiStatus}")}
        Text("Optional AI: Built with Llama. Only public source facts are sent to Cloudflare; no reading data is uploaded.",style=MaterialTheme.typography.bodySmall)
        Text("GitHub schedules may be delayed or disabled after 60 days without repository activity. Check the Actions page if editions stop arriving.")
        HorizontalDivider()
        Text("Change PIN",style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(newPin,{newPin=it.filter(Char::isDigit).take(8)},label={Text("New PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword))
        OutlinedTextField(repeatPin,{repeatPin=it.filter(Char::isDigit).take(8)},label={Text("Repeat new PIN")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword))
        OutlinedButton(onClick={vm.changePin(newPin);newPin="";repeatPin="";vm.message.value="Parent PIN changed"},enabled=newPin.length>=4 && newPin==repeatPin){Text("Change parent PIN")}
        OutlinedButton(onClick={clear=true}){Text("Clear downloaded editions and progress")}
        if(BuildConfig.DEBUG)OutlinedButton(onClick={vm.sample()}){Text("Load labelled sample for testing")}
    }
    if(generate)AlertDialog(onDismissRequest={generate=false},title={Text("Generate Today's News")},
        text={Text("Opens GitHub Actions. Sign in as the parent, then press Run workflow to generate today's edition. Return here and tap Check for Today's Edition.")},
        confirmButton={TextButton(onClick={generate=false;vm.message.value="Opening GitHub. Press Run workflow there.";openLink(saved.workflow)}){Text("Open GitHub Actions")}},
        dismissButton={TextButton(onClick={generate=false}){Text("Cancel")}})
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("Clear local reading data?")},text={Text("Downloaded editions, reading progress and quiz answers will be removed. Your PIN and connection settings stay saved.")},
        confirmButton={TextButton(onClick={clear=false;vm.clear()}){Text("Clear")}},dismissButton={TextButton(onClick={clear=false}){Text("Cancel")}})
}
