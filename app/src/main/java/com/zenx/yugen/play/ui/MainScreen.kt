package com.zenx.yugen.play.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zenx.yugen.play.ui.calendar.CalendarScreen
import com.zenx.yugen.play.ui.components.FloatingAnimatedBottomBar
import com.zenx.yugen.play.ui.detail.DetailScreen
import com.zenx.yugen.play.ui.home.HomeScreen
import com.zenx.yugen.play.ui.library.LibraryScreen
import com.zenx.yugen.play.ui.player.PlayerScreen
import com.zenx.yugen.play.ui.profile.ProfileScreen
import com.zenx.yugen.play.ui.search.SearchScreen
import com.zenx.yugen.play.ui.settings.SettingsScreen

sealed class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Calendar : BottomNavItem("calendar", "Calendar", Icons.Filled.DateRange, Icons.Outlined.DateRange)
    data object Library : BottomNavItem("library", "Library", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    data object Downloads : BottomNavItem("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download)
}

private fun buildPlayerRoute(
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
fun MainScreen(
    pendingNavRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(pendingNavRoute) {
        if (!pendingNavRoute.isNullOrBlank()) {
            try {
                navController.navigate(pendingNavRoute)
                onRouteHandled()
            } catch (_: Exception) {}
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.Library,
        BottomNavItem.Downloads
    )
    val bottomNavRoutes = remember { bottomNavItems.map { it.route } }

    val activeTabRoute = remember(currentRoute) {
        when {
            currentRoute?.startsWith(BottomNavItem.Calendar.route) == true -> BottomNavItem.Calendar.route
            currentRoute?.startsWith(BottomNavItem.Library.route) == true -> BottomNavItem.Library.route
            currentRoute?.startsWith(BottomNavItem.Downloads.route) == true -> BottomNavItem.Downloads.route
            else -> BottomNavItem.Home.route
        }
    }

    val showBottomBar = currentRoute in bottomNavRoutes

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF09090B))) {

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val fromIndex = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIndex = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIndex != -1 && toIndex != -1) {
                    val towards = if (toIndex > fromIndex) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                    slideIntoContainer(
                        towards = towards,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(200))
                }
            },
            exitTransition = {
                val fromIndex = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIndex = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIndex != -1 && toIndex != -1) {
                    val towards = if (toIndex > fromIndex) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                    slideOutOfContainer(
                        towards = towards,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(200))
                }
            },
            popEnterTransition = {
                val fromIndex = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIndex = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIndex != -1 && toIndex != -1) {
                    val towards = if (toIndex > fromIndex) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                    slideIntoContainer(
                        towards = towards,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(200))
                }
            },
            popExitTransition = {
                val fromIndex = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIndex = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIndex != -1 && toIndex != -1) {
                    val towards = if (toIndex > fromIndex) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End
                    slideOutOfContainer(
                        towards = towards,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(200))
                }
            }
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onSearchClick = { navController.navigate("search") },
                    onAnimeClick = { id, title, posterUrl -> navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}") },
                    onHistoryClick = { episodeId, title, poster ->
                        if (episodeId.startsWith("CLOUD_SYNC_")) {
                            val mediaId = episodeId.removePrefix("CLOUD_SYNC_").substringBefore("_")
                            navController.navigate("detail?id=${Uri.encode(mediaId)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}")
                        } else {
                            navController.navigate(buildPlayerRoute(episodeId = episodeId, title = title, poster = poster))
                        }
                    },
                    onProfileClick = { navController.navigate("profile") },
                    onSettingsClick = { navController.navigate("settings") },
                    onTrendingViewAll = { navController.navigate("search?sort=TRENDING_DESC") },
                    onAiringViewAll = {
                        navController.navigate(BottomNavItem.Calendar.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(
                    onAnimeClick = { id, title, posterUrl -> navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}") }
                )
            }

            composable(
                route = "search?sort={sort}",
                arguments = listOf(navArgument("sort") { type = NavType.StringType; nullable = true })
            ) {
                SearchScreen(
                    onAnimeClick = { id, title, posterUrl ->
                        navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(BottomNavItem.Library.route) {
                LibraryScreen(
                    onAnimeClick = { id, title, posterUrl -> navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}") },
                    onHistoryClick = { episodeId, title, poster ->
                        if (episodeId.startsWith("CLOUD_SYNC_")) {
                            val mediaId = episodeId.removePrefix("CLOUD_SYNC_").substringBefore("_")
                            navController.navigate("detail?id=${Uri.encode(mediaId)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}")
                        } else {
                            navController.navigate(buildPlayerRoute(episodeId = episodeId, title = title, poster = poster))
                        }
                    }
                )
            }

            composable(BottomNavItem.Downloads.route) {
                com.zenx.yugen.play.ui.downloads.DownloadsScreen(
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { episodeId, animeUrl, title, poster ->
                        navController.navigate(buildPlayerRoute(episodeId = episodeId, animeUrl = animeUrl, title = title, poster = poster))
                    }
                )
            }

            composable("settings") {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }

            composable("profile") {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onAnimeClick = { id, title, posterUrl -> navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}") }
                )
            }

            composable(
                route = "detail?id={id}&url={url}&title={title}&poster={poster}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; defaultValue = "" },
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("poster") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                DetailScreen(
                    onEpisodeClick = { episodeId, animeUrl, title, poster, streamUrl ->
                        navController.navigate(buildPlayerRoute(episodeId = episodeId, animeUrl = animeUrl, title = title, poster = poster, streamUrl = streamUrl))
                    },
                    onBackClick = { navController.popBackStack() },
                    onDownloadsClick = {
                        navController.navigate(BottomNavItem.Downloads.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

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
                PlayerScreen(onBackClick = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingAnimatedBottomBar(
                    items = bottomNavItems,
                    currentRoute = activeTabRoute,
                    onItemClick = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}