package com.zenx.yugen.play.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zenx.yugen.play.ui.calendar.CalendarScreen
import com.zenx.yugen.play.ui.components.bounceClick
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

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.Library,
        BottomNavItem.Downloads
    )
    val bottomNavRoutes = remember { bottomNavItems.map { it.route } }

    var activeTabRoute by remember { mutableStateOf(BottomNavItem.Home.route) }

    // FIX 1: Synchronous state calculation. No more LaunchedEffect 1-frame jitter.
    val showBottomBar = currentRoute in bottomNavRoutes
    remember(currentRoute) {
        if (showBottomBar && currentRoute != null) {
            activeTabRoute = currentRoute
        }
        true
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF09090B))) {

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                    fadeIn(tween(250))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn()
                }
            },
            exitTransition = {
                if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                    fadeOut(tween(250))
                } else {
                    fadeOut(tween(300))
                }
            },
            popEnterTransition = {
                if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                    fadeIn(tween(250))
                } else {
                    fadeIn(tween(300))
                }
            },
            popExitTransition = {
                if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                    fadeOut(tween(250))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut()
                }
            }
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onSearchClick = { navController.navigate("search") },
                    onAnimeClick = { id, title, posterUrl -> navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}") },
                    onHistoryClick = { episodeId, title, poster -> navController.navigate("player/${Uri.encode(episodeId)}?animeUrl=&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}&streamUrl=") },
                    onProfileClick = { navController.navigate("profile") },
                    onSettingsClick = { navController.navigate("settings") },
                    onTrendingViewAll = { navController.navigate("search") },
                    onAiringViewAll = { navController.navigate(BottomNavItem.Calendar.route) }
                )
            }

            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(
                    onAnimeClick = { id, title, posterUrl -> navController.navigate("detail?id=${Uri.encode(id)}&url=&title=${Uri.encode(title)}&poster=${Uri.encode(posterUrl)}") }
                )
            }

            composable("search") {
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
                    onHistoryClick = { episodeId, title, poster -> navController.navigate("player/${Uri.encode(episodeId)}?animeUrl=&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}&streamUrl=") }
                )
            }

            composable(BottomNavItem.Downloads.route) {
                com.zenx.yugen.play.ui.downloads.DownloadsScreen(
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { episodeId, animeUrl, title, poster ->
                        navController.navigate("player/${Uri.encode(episodeId)}?animeUrl=${Uri.encode(animeUrl)}&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}&streamUrl=")
                    }
                )
            }

            composable("settings") { SettingsScreen() }

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
                        val encodedStream = streamUrl?.let { Uri.encode(it) } ?: ""
                        navController.navigate("player/${Uri.encode(episodeId)}?animeUrl=${Uri.encode(animeUrl)}&title=${Uri.encode(title)}&poster=${Uri.encode(poster)}&streamUrl=$encodedStream")
                    },
                    onBackClick = { navController.popBackStack() },
                    onDownloadsClick = { navController.navigate(BottomNavItem.Downloads.route) }
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
                        if (activeTabRoute != route) {
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

@Composable
fun FloatingAnimatedBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    val glassPillBg = Color(0xFF141416).copy(alpha = 0.85f)
    val glassBorder = Color.White.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            // FIX 2: Removed .animateContentSize() from the parent Row.
            // It was creating a recursive layout calculation loop with the child items.
            .clip(RoundedCornerShape(100.dp))
            .background(glassPillBg)
            .border(1.dp, glassBorder, RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            AnimatedBottomBarItem(
                item = item,
                isSelected = currentRoute == item.route,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}

@Composable
fun AnimatedBottomBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentPurple = Color(0xFF8B5CF6)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentPurple.copy(alpha = 0.15f) else Color.Transparent,
        label = "bottom_bar_bg_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accentPurple else Color.Gray,
        label = "bottom_bar_content_color"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .bounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        if (isSelected) {
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}