package com.zenx.yugen.play.ui.auth

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnilistLoginDialog(
    onDismiss: () -> Unit,
    onTokenReceived: (String) -> Unit,
    avatarUrl: String? = null,
    username: String? = null,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val accentBlue = Color(0xFF3DB4F2) // AniList Brand Blue
    val surfaceColor = Color(0xFF141416)

    var showPinField by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            showPinField = true
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(surfaceColor)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (avatarUrl != null && username != null) {
                // Logged In State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, accentBlue, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Logged in as", color = Color.Gray, fontSize = 13.sp)
                    Text(username, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onLogout()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Login Options State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("AniList Sync", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect your account to automatically track watched episodes and sync your library.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    AnimatedVisibility(
                        visible = !errorMessage.isNullOrBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Option 1: Auto Login (Deep Link)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentBlue.copy(alpha = 0.15f))
                            .clickable(enabled = !isLoading) {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://anilist.co/api/v2/oauth/authorize?client_id=48068&response_type=token".toUri()
                                )
                                context.startActivity(intent)
                                onDismiss()
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = accentBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Auto Login", color = accentBlue, fontWeight = FontWeight.Bold)
                                Text("Recommended • Uses browser", color = accentBlue.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: PIN Login
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable(enabled = !isLoading) {
                                showPinField = true
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://anilist.co/api/v2/oauth/authorize?client_id=48327&response_type=token".toUri()
                                )
                                context.startActivity(intent)
                            }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("PIN Login", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Manual copy-paste code", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    AnimatedVisibility(visible = showPinField) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            OutlinedTextField(
                                value = pinText,
                                onValueChange = { pinText = it },
                                enabled = !isLoading,
                                placeholder = { Text("Paste PIN here", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (pinText.isNotBlank() && !isLoading) {
                                                onTokenReceived(pinText.trim())
                                            }
                                        },
                                        enabled = !isLoading && pinText.isNotBlank()
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = accentBlue,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.ContentPaste,
                                                contentDescription = "Submit",
                                                tint = if (pinText.isNotBlank()) accentBlue else Color.Gray
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}