package uk.family.mydailyworld

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val model: WorldViewModel by viewModels()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { model.message.value =
        if(it) "Notifications are allowed" else "Notifications are off. You can still read every downloaded edition." }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val p by model.settings.collectAsStateWithLifecycle()
            val dark = p.theme == "Dark" || (p.theme == "System" && isSystemInDarkTheme())
            val scheme = if(dark) darkColorScheme(primary=Color(0xFF81D5CD),secondary=Color(0xFFACC8FF))
                else lightColorScheme(primary=Color(0xFF006A68),secondary=Color(0xFF365D86),
                    background=Color(0xFFF7F8F4),surface=Color(0xFFF7F8F4),surfaceVariant=Color(0xFFE4EEEA))
            MaterialTheme(colorScheme=scheme) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density,density.fontScale*p.font)) {
                    WorldApp(model,openLink={ openBrowser(it) },askNotifications={
                        if(Build.VERSION.SDK_INT>=33)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else model.message.value="Notifications are available in Android settings"
                    })
                }
            }
        }
    }
    private fun openBrowser(url:String) {
        if(!publicHttps(url)) {model.message.value="A valid HTTPS address is needed";return}
        try {startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}
        catch (_:Exception){model.message.value="No browser is available"}
    }
    override fun onStart(){super.onStart();model.sync()}
    override fun onResume(){super.onResume();model.startPolling()}
    override fun onStop(){model.lock();model.stopPolling();super.onStop()}
}
