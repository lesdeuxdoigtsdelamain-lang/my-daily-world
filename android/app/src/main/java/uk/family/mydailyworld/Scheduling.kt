package uk.family.mydailyworld

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class WorldApplication : Application() {
    lateinit var database: WorldDatabase; private set
    lateinit var preferences: ParentPreferences; private set
    lateinit var repository: NewsRepository; private set
    override fun onCreate() {
        super.onCreate()
        database = androidx.room.Room.databaseBuilder(this,WorldDatabase::class.java,"daily-world.db").build()
        preferences = ParentPreferences(this)
        repository = NewsRepository(database.worldDao(),preferences,ContentApi(java.io.File(cacheDir,"http")))
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("daily", "Daily edition", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A reminder only when today's downloaded edition is ready"
        })
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            if(!preferences.bundledEditionImported()) {
                // A genuine, cited, dated edition is bundled for the first offline read.
                // It is never relabelled as current on a different date.
                val bundled=runCatching {parseEdition(assets.open("initial-edition.json").bufferedReader().readText())}.getOrNull()
                if(bundled!=null && !java.time.LocalDate.parse(bundled.date).isAfter(today()) && database.worldDao().latest()==null) {
                    database.worldDao().put(CachedEdition(bundled.id,bundled.date,wire.encodeToString(bundled)))
                }
                preferences.markBundledImported()
            }
            Scheduler.schedule(this@WorldApplication,preferences.flow.first())
        }
    }
}
val Context.world: WorldApplication get() = applicationContext as WorldApplication

object Scheduler {
    fun schedule(context: Context,p: Preferences) {
        val manager = context.getSystemService(AlarmManager::class.java)
        for((code,preload) in listOf(1700 to false,1645 to true)) {
            val pending = PendingIntent.getBroadcast(context,code,Intent(context,ReminderReceiver::class.java)
                .putExtra("preload",preload),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.cancel(pending)
            if(p.reminders || preload) {
                val time = if(preload) LocalTime.of(p.hour,p.minute).minusMinutes(15) else LocalTime.of(p.hour,p.minute)
                val at = nextReminder(Instant.now(),time.hour,time.minute).toEpochMilli()
                // News does not justify a privileged exact-alarm permission.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending)
            }
        }
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(1,TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily-sync",ExistingPeriodicWorkPolicy.KEEP,periodic)
    }
    fun sync(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("check-edition",ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.MINUTES).build())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {
                context.world.repository.latestValidated()?.let { Notifier.edition(context,it) }
                Scheduler.sync(context)
                Scheduler.schedule(context,context.world.preferences.flow.first())
            } finally { pending.finish() }
        }
    }
}
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try { Scheduler.schedule(context,context.world.preferences.flow.first());Scheduler.sync(context) }
            finally {pending.finish()}
        }
    }
}
class SyncWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val result=applicationContext.world.repository.sync()
        applicationContext.world.repository.latestValidated()?.let {Notifier.edition(applicationContext,it)}
        return if(result.failed && runAttemptCount<2) Result.retry() else Result.success()
    }
}

object Notifier {
    fun allowed(context: Context): Boolean = (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,
        Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel("daily")?.importance != NotificationManager.IMPORTANCE_NONE

    private fun build(context: Context,title: String,text: String): Notification {
        val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context,"daily").setSmallIcon(R.drawable.ic_world)
            .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build()
    }
    suspend fun edition(context: Context,e: Edition,now: Instant = Instant.now()): Boolean {
        val p=context.world.preferences.flow.first()
        if(!p.reminders || !allowed(context) || !shouldNotify(e,now,p.hour,p.minute)) return false
        // Reconfirm cache before claiming; callers cannot announce unpersisted content.
        if(context.world.database.worldDao().edition(e.id)==null) return false
        val dao=context.world.database.worldDao()
        if(dao.claimNotification(Notified(e.id)) == -1L) return false
        return try {
            context.getSystemService(NotificationManager::class.java).notify(e.id.hashCode(),
                build(context,"Your Daily World is ready!","Discover today's stories 🌍"))
            true
        } catch (_:SecurityException) { dao.undoClaim(e.id);false }
    }
    fun test(context: Context): Boolean {
        if(!allowed(context))return false
        return try {
            context.getSystemService(NotificationManager::class.java).notify(42,
                build(context,"My Daily World — test","This is a test reminder, not a new edition."));true
        } catch (_:SecurityException){false}
    }
}
