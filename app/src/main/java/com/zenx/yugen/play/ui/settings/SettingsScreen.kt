package com.zenx.yugen.play.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.zenx.yugen.play.ui.components.bounceClick
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val baseBackground = Color(0xFF09090B)
    val glassBg = Color.White.copy(alpha = 0.06f)
    val glassBorder = Color.White.copy(alpha = 0.12f)
    val accentRed = Color(0xFFEF4444)
    val dialogBg = Color(0xFF141416)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = baseBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
            }

            item { Text("Data & Storage", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }

            item {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(glassBg).border(1.dp, glassBorder, RoundedCornerShape(16.dp))) {
                    SettingsItem(icon = Icons.Default.Refresh, title = "Clear Image Cache", subtitle = "Free up storage used by cached posters and banners", onClick = { showClearCacheDialog = true })
                    HorizontalDivider(color = glassBorder)
                    SettingsItem(icon = Icons.Default.Delete, title = "Clear Watch History", subtitle = "Wipe all playback positions and watched episodes", titleColor = accentRed, onClick = { showClearHistoryDialog = true })
                }
            }

            item { Text("Application", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }

            item {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(glassBg).border(1.dp, glassBorder, RoundedCornerShape(16.dp))) {
                    SettingsItem(icon = Icons.Default.Info, title = "App Version", subtitle = "v1.0.0-alpha (Kotlin + Compose)", onClick = {})
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false }, containerColor = dialogBg, shape = RoundedCornerShape(20.dp),
            title = { Text("Clear Image Cache?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all downloaded poster and banner images. They will reload as needed.", color = Color.LightGray) },
            confirmButton = { TextButton(onClick = { showClearCacheDialog = false; viewModel.clearImageCache { coroutineScope.launch { snackbarHostState.showSnackbar("Image cache cleared.") } } }) { Text("Clear", color = accentRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel", color = Color.White) } }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false }, containerColor = dialogBg, shape = RoundedCornerShape(20.dp),
            title = { Text("Wipe Watch History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all episode progress records from your local database.", color = Color.LightGray) },
            confirmButton = { TextButton(onClick = { showClearHistoryDialog = false; viewModel.clearWatchHistory { coroutineScope.launch { snackbarHostState.showSnackbar("Watch history deleted.") } } }) { Text("Delete All", color = accentRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel", color = Color.White) } }
        )
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, titleColor: Color = Color.White, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().bounceClick { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color.Gray, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}