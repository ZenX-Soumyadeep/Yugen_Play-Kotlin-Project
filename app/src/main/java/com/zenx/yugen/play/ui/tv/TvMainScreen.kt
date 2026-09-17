package com.zenx.yugen.play.ui.tv

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zenx.yugen.play.ui.tv.player.TvPlayerScreen
import com.zenx.yugen.play.ui.tv.calendar.TvCalendarScreen
import com.zenx.yugen.play.ui.tv.components.TvNavItem
import com.zenx.yugen.play.ui.tv.components.TvNavigationRail
import com.zenx.yugen.play.ui.tv.detail.TvDetailScreen
import com.zenx.yugen.play.ui.tv.home.TvHomeScreen
import com.zenx.yugen.play.ui.tv.library.TvLibraryScreen
import com.zenx.yugen.play.ui.tv.search.TvSearchScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.zenx.yugen.play.BuildConfig
import com.zenx.yugen.play.ui.tv.updater.TvUpdateDialog
import com.zenx.yugen.play.ui.updater.UpdateViewModel
import com.zenx.yugen.play.ui.auth.AuthViewModel
import com.zenx.yugen.play.ui.tv.settings.TvSettingsScreen
import com.zenx.yugen.play.ui.tv.downloads.TvDownloadsScreen
import com.zenx.yugen.play.ui.tv.profile.TvProfileScreen

private fun buildTvPlayerRoute(
    episodeId: String,
    animeUrl: String = "",
    title: String = "",
    poster: String = "",
    streamUrl: String? = null
): String {
    val epId = Uri.encode(episodeId)
    val aUrl = Uri.encode(animeUrl)
    val t = Uri.encode(title)
    val p = Uri.encode(poster)
    val s = streamUrl?.takeIf { it.isNotBlank() }?.let { Uri.encode(it) } ?: ""
    return "player/$epId?animeUrl=$aUrl&title=$t&poster=$p&streamUrl=$s"
}

@Composable
fun TvMainScreen(
    pendingNavRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()

    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var hasPromptedUpdateOnLaunch by rememberSaveable { mutableStateOf(false) }

    // Automatic update check on TV first launch
    LaunchedEffect(updateInfo) {
        if (updateInfo != null && !hasPromptedUpdateOnLaunch) {
            hasPromptedUpdateOnLaunch = true
            showUpdateDialog = true
        }
    }

    LaunchedEffect(pendingNavRoute) {
        if (!pendingNavRoute.isNullOrBlank()) {
            try {
                navController.navigate(pendingNavRoute)
                onRouteHandled()
            } catch (_: Exception) {}
        }
    }

    val activeRailRoute = remember(currentRoute) {
        when {
            currentRoute?.startsWith("search") == true -> "search"
            currentRoute?.startsWith("calendar") == true -> "calendar"
            currentRoute?.startsWith("library") == true -> "library"
            currentRoute?.startsWith("downloads") == true -> "downloads"
            currentRoute?.startsWith("settings") == true -> "settings"
            currentRoute?.startsWith("profile") == true -> "profile"
            else -> "home"
        }
    }

    // Hide rail on full-screen media detail and player
    val showRail = currentRoute?.startsWith("detail") != true && currentRoute?.startsWith("player") != true

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C))
    ) {
        // Left Docked Collapsing Navigation Rail
        AnimatedVisibility(
            visible = showRail,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            TvNavigationRail(
                selectedRoute = activeRailRoute,
                onSelectRoute = { route ->
                    if (activeRailRoute != route) {
                        navController.navigate(route) {
                            popUpTo("home") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                userAvatarUrl = if (authState.isAuthenticated) authState.avatarUrl else null,
                userName = if (authState.isAuthenticated) authState.username else null,
                onProfileClick = {
                    if (activeRailRoute != "profile") {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // TV Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            NavHost(
                navController = navController,
                startDestination = TvNavItem.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(200)) }
            ) {
                // --- HOME ---
                composable(TvNavItem.Home.route) {
                    TvHomeScreen(
                        onAnimeClick = { id, title, posterUrl ->
                            navController.navigate(
                                "detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}"
                            )
                        },
                        onContinueWatchingClick = { episodeId, title, poster ->
                            if (episodeId.startsWith("CLOUD_SYNC_")) {
                                val mediaId = episodeId.removePrefix("CLOUD_SYNC_").substringBefore("_")
                                navController.navigate(
                                    "detail?id=${Uri.encode(mediaId)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}"
                                )
                            } else {
                                navController.navigate(
                                    buildTvPlayerRoute(episodeId = episodeId, title = title, poster = poster)
                                )
                            }
                        }
                    )
                }

                // --- SEARCH ---
                composable(
                    route = "search?sort={sort}&query={query}&genre={genre}",
                    arguments = listOf(
                        navArgument("sort") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("genre") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) {
                    TvSearchScreen(
                        onAnimeClick = { id, title, posterUrl ->
                            navController.navigate(
                                "detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}"
                            )
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // --- SCHEDULE / CALENDAR ---
                composable(TvNavItem.Schedule.route) {
                    TvCalendarScreen(
                        onAnimeClick = { id, title, posterUrl ->
                            navController.navigate(
                                "detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}"
                            )
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // --- LIBRARY ---
                composable(TvNavItem.Library.route) {
                    TvLibraryScreen(
                        onAnimeClick = { id, title, posterUrl ->
                            navController.navigate(
                                "detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}"
                            )
                        },
                        onHistoryClick = { episodeId, title, poster ->
                            if (episodeId.startsWith("CLOUD_SYNC_")) {
                                val mediaId = episodeId.removePrefix("CLOUD_SYNC_").substringBefore("_")
                                navController.navigate(
                                    "detail?id=${Uri.encode(mediaId)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}"
                                )
                            } else {
                                navController.navigate(
                                    buildTvPlayerRoute(episodeId = episodeId, title = title, poster = poster)
                                )
                            }
                        }
                    )
                }

                // --- DOWNLOADS ---
                composable(TvNavItem.Downloads.route) {
                    TvDownloadsScreen(
                        onPlayEpisode = { episodeId, animeTitle, posterUrl ->
                            navController.navigate(
                                buildTvPlayerRoute(episodeId = episodeId, title = animeTitle, poster = posterUrl)
                            )
                        },
                        onBrowseClick = {
                            navController.navigate(TvNavItem.Home.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                // --- SETTINGS ---
                composable(TvNavItem.Settings.route) {
                    TvSettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onCheckForUpdates = {
                            updateViewModel.checkForUpdates(force = true)
                            if (updateInfo != null) {
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(context, "Checking for latest updates...", Toast.LENGTH_SHORT).show()
                                showUpdateDialog = true
                            }
                        }
                    )
                }

                // --- TV ANIME DETAILS ---
                composable(
                    route = "detail?id={id}&url={url}&title={title}&poster={poster}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType; defaultValue = "" },
                        navArgument("url") { type = NavType.StringType; defaultValue = "" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "" },
                        navArgument("poster") { type = NavType.StringType; defaultValue = "" }
                    )
                ) {
                    TvDetailScreen(
                        onEpisodeClick = { episodeId, animeUrl, title, poster, streamUrl ->
                            navController.navigate(
                                buildTvPlayerRoute(
                                    episodeId = episodeId,
                                    animeUrl = animeUrl,
                                    title = title,
                                    poster = poster,
                                    streamUrl = streamUrl
                                )
                            )
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // --- PLAYER ---
                composable(
                    route = "player/{episodeId}?animeUrl={animeUrl}&title={title}&poster={poster}&streamUrl={streamUrl}",
                    arguments = listOf(
                        navArgument("episodeId") { type = NavType.StringType },
                        navArgument("animeUrl") { type = NavType.StringType; defaultValue = "" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "Unknown Anime" },
                        navArgument("poster") { type = NavType.StringType; defaultValue = "" },
                        navArgument("streamUrl") { type = NavType.StringType; defaultValue = "" }
                    )
                ) {
                    TvPlayerScreen(onBackClick = { navController.popBackStack() })
                }

                // --- TV PROFILE ---
                composable("profile") {
                    TvProfileScreen(
                        onAnimeClick = { id, title, posterUrl ->
                            navController.navigate(
                                "detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}"
                            )
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        TvUpdateDialog(
            updateInfo = updateInfo!!,
            downloadState = downloadState,
            onDismiss = { showUpdateDialog = false },
            onStartDownload = { url -> updateViewModel.downloadAndInstallApk(url) },
            onInstallApk = { file -> updateViewModel.installApk(file) },
            onCancelDownload = { updateViewModel.cancelDownload() },
            onResetDownloadState = { updateViewModel.resetDownloadState() }
        )
    }
}
