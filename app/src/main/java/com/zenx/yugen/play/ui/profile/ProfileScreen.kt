package com.zenx.yugen.play.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zenx.yugen.play.domain.AnilistListEntry
import com.zenx.yugen.play.ui.components.bounceClick
import com.zenx.yugen.play.ui.components.subtleMarquee
import com.zenx.yugen.play.ui.home.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onAnimeClick: (id: String, title: String, posterUrl: String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val baseBackground = Color(0xFF09090B)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val accentPurple = Color(0xFF8B5CF6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (uiState !is ProfileUiState.Unauthenticated) {
                        IconButton(onClick = { viewModel.logout(); onBackClick() }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFFEF4444))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = baseBackground
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> CircularProgressIndicator(color = accentPurple, modifier = Modifier.align(Alignment.Center))
                is ProfileUiState.Unauthenticated -> Text("Please login from the Home screen.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                is ProfileUiState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                is ProfileUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                                if (state.user.banner != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(state.user.banner).crossfade(300).build(),
                                        contentDescription = "Banner", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(180.dp)
                                    )
                                    Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(Color.Transparent, baseBackground))))
                                }

                                Row(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp), verticalAlignment = Alignment.Bottom) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(state.user.avatar).crossfade(300).build(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.size(100.dp).clip(CircleShape).border(2.dp, glassBorder, CircleShape)
                                    )
                                    Column(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
                                        Text(state.user.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${state.user.animeCount} Anime  •  ${state.user.episodesWatched} Eps", color = accentPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        val listOrder = listOf("Watching", "Completed", "Paused", "Dropped", "Planning")
                        listOrder.forEach { listName ->
                            val entries = state.animeLists[listName]
                            if (!entries.isNullOrEmpty()) {
                                item {
                                    SectionHeader(title = listName, accentColor = accentPurple)
                                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(entries) { entry ->
                                            AnilistEntryCard(entry = entry, accentColor = accentPurple, onClick = { onAnimeClick(entry.mediaId.toString(), entry.title, entry.posterUrl) })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnilistEntryCard(entry: AnilistListEntry, accentColor: Color, onClick: () -> Unit) {
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val context = LocalContext.current
    Column(modifier = Modifier.width(130.dp).bounceClick { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(12.dp)).border(1.dp, glassBorder, RoundedCornerShape(12.dp))) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(entry.posterUrl).crossfade(300).build(),
                contentDescription = entry.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                val epText = if (entry.totalEpisodes != null) "${entry.progress} / ${entry.totalEpisodes}" else "${entry.progress} / ?"
                Text(epText, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(entry.title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(top = 8.dp, start = 2.dp).fillMaxWidth().subtleMarquee())
    }
}