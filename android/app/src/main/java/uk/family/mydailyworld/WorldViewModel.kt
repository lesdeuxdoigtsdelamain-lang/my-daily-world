package uk.family.mydailyworld

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class WorldViewModel(app: Application): AndroidViewModel(app) {
    private val world=app as WorldApplication
    val dao=world.database.worldDao()
    val settings=world.preferences.flow.stateIn(viewModelScope,SharingStarted.Eagerly,Preferences())
    val cached=dao.editions().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val read=dao.readings().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val answers=dao.answers().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val checking=MutableStateFlow(false)
    val message=MutableStateFlow("")
    val unlocked=MutableStateFlow(false)
    private var polling: Job?=null
    private var syncJob: Job?=null

    fun sync() {
        if(syncJob?.isActive==true)return
        syncJob=viewModelScope.launch {
            checking.value=true;message.value="Checking…"
            try {
                val r=world.repository.sync();message.value=r.message
                world.repository.latestValidated()?.let {Notifier.edition(world,it)}
            } finally {checking.value=false}
        }
    }
    fun startPolling() {
        polling?.cancel()
        polling=viewModelScope.launch {
            // Four checks over two minutes, only while the activity is visible.
            repeat(4) {
                sync(); delay(30_000)
                if(cached.value.any {it.date==today().toString()}) { polling?.cancel();return@launch }
            }
        }
    }
    fun stopPolling(){polling?.cancel()}
    fun lock(){unlocked.value=false}
    fun pin(pin: String, create: Boolean, callback:(Boolean)->Unit) = viewModelScope.launch(Dispatchers.Default) {
        val success=if(create) {world.preferences.setPin(pin);true} else world.preferences.verify(pin)
        unlocked.value=success
        withContext(Dispatchers.Main){callback(success)}
    }
    fun changePin(pin: String) = viewModelScope.launch(Dispatchers.Default){world.preferences.setPin(pin)}
    fun save(p: Preferences) = viewModelScope.launch {
        world.preferences.save(p);Scheduler.schedule(world,p);dao.trim(p.keep);message.value="Settings saved"
    }
    fun mark(id:String,story:String,read:Boolean) = viewModelScope.launch {
        if(read)dao.mark(ReadState(id,story)) else dao.unmark(id,story)
    }
    fun answer(id:String,q:String,choice:Int) = viewModelScope.launch {dao.answer(Answer(id,q,choice))}
    fun retry(id:String) = viewModelScope.launch {dao.retryQuiz(id)}
    fun position(id:String,index:Int) = viewModelScope.launch {dao.savePosition(id,index)}
    fun clear() = viewModelScope.launch {dao.clearCache();message.value="Downloaded editions and reading progress cleared"}
    fun sample() = viewModelScope.launch(Dispatchers.IO) {
        if(!BuildConfig.DEBUG || !unlocked.value)return@launch
        val e=parseEdition(world.assets.open("sample.json").bufferedReader().readText(),allowSample=true)
        dao.put(CachedEdition(e.id,e.date,wire.encodeToString(e)))
        message.value="SAMPLE — NOT CURRENT NEWS"
    }
}
