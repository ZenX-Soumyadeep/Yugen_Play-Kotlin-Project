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
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authPreferences: AuthPreferences

    @Inject
    lateinit var anilistService: AnilistService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        handleAuthIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        var keepSplashOpen by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { keepSplashOpen }

        setContent {
            YugenPlayTheme {
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
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .zIndex(1f)
                        ) {
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
                val params = fragment.split("&").mapNotNull { entry ->
                    val eqIdx = entry.indexOf('=')
                    if (eqIdx != -1) {
                        val key = entry.substring(0, eqIdx)
                        val value = entry.substring(eqIdx + 1)
                        key to value
                    } else {
                        null
                    }
                }.toMap()

                val token = params["access_token"]
                if (token != null) {
                    lifecycleScope.launch {
                        try {
                            val user = anilistService.getAuthenticatedUser(token)
                            if (user != null) {
                                authPreferences.saveAuth(token, user.id, user.name, user.avatar)
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Failed to retrieve authenticated user", e)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalNetworkBanner(context: Context) {
    val connectivityManager = remember(context) {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    val initialConnected = remember(connectivityManager) {
        val activeNet = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNet)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    val isConnected by produceState(initialValue = initialConnected, context) {
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { trySend(true) }
                override fun onLost(network: Network) { trySend(false) }
            }

            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.collect { value = it }
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