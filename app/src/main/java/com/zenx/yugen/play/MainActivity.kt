package com.zenx.yugen.play

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity // BROUGHT BACK
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.zenx.yugen.play.data.local.AuthPreferences
import com.zenx.yugen.play.data.remote.AnilistService
import com.zenx.yugen.play.ui.MainScreen
import com.zenx.yugen.play.ui.theme.YugenPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() { // MUST BE AppCompatActivity for Google Cast

    @Inject
    lateinit var authPreferences: AuthPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Silently handle. If granted, download notifications will show.
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        handleAuthIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Keep the splash screen visible until Compose is ready
        var keepSplashOpen by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { keepSplashOpen }

        setContent {
            YugenPlayTheme {
                // 3. Dismiss splash and ask for permissions once UI mounts
                LaunchedEffect(Unit) {
                    keepSplashOpen = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen()

                    val isFullscreen = WindowInsets.statusBars.getTop(LocalDensity.current) == 0

                    if (!isFullscreen) {
                        Box(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().zIndex(1f)) {
                            GlobalNetworkBanner(context = LocalContext.current)
                        }
                    }
                }
            }
        }
    }

    private fun handleAuthIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "yugenplay" && uri.host == "auth") {
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate {
                    val parts = it.split("=")
                    parts[0] to if (parts.size > 1) parts[1] else ""
                }
                val token = params["access_token"]
                if (token != null) {
                    lifecycleScope.launch {
                        val user = AnilistService.getAuthenticatedUser(token)
                        if (user != null) {
                            authPreferences.saveAuth(token, user.id, user.name, user.avatar)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalNetworkBanner(context: Context) {
    var isConnected by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected = true }
            override fun onLost(network: Network) { isConnected = false }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    AnimatedVisibility(
        visible = !isConnected,
        enter = expandVertically(animationSpec = tween(300)),
        exit = shrinkVertically(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE53935))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No Internet Connection",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}