package com.zenx.yugen.play.ui.player.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.zenx.yugen.play.util.DeviceController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerGestureOverlay(
    deviceController: DeviceController,
    isLocked: Boolean,
    durationMs: Long,
    currentPositionMs: Long,
    onToggleControls: () -> Unit,
    onSeekRelative: (seconds: Int) -> Unit,
    onSeekScrub: (positionMs: Long) -> Unit,
    onSeekScrubEnd: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var hudHideJob by remember { mutableStateOf<Job?>(null) }
    var rippleHideJob by remember { mutableStateOf<Job?>(null) }

    var hudState by remember { mutableStateOf(HudState()) }
    var showForwardRipple by remember { mutableStateOf(false) }
    var showRewindRipple by remember { mutableStateOf(false) }

    var isDragVertical by remember { mutableStateOf(false) }
    var isDragHorizontal by remember { mutableStateOf(false) }
    var isLeftSideDrag by remember { mutableStateOf(false) }

    var isValidVerticalDragZone by remember { mutableStateOf(false) }
    var isIgnoredDrag by remember { mutableStateOf(false) }

    var scrubTargetPos by remember { mutableLongStateOf(0L) }
    var lastSeekUpdate by remember { mutableLongStateOf(0L) }

    var accumulatedDx by remember { mutableFloatStateOf(0f) }
    var accumulatedDy by remember { mutableFloatStateOf(0f) }

    var initialVolume by remember { mutableIntStateOf(0) }
    var initialBrightness by remember { mutableFloatStateOf(0.5f) }
    var initialSeekPosition by remember { mutableLongStateOf(0L) }

    val livePositionMs by rememberUpdatedState(currentPositionMs)
    val liveDurationMs by rememberUpdatedState(durationMs)

    fun showHud(type: HudType, value: Float, text: String) {
        hudHideJob?.cancel()
        hudState = HudState(isVisible = true, type = type, value = value, centerText = text)
        hudHideJob = coroutineScope.launch {
            delay(900L.milliseconds)
            hudState = hudState.copy(isVisible = false)
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        val hours = totalSec / 3600
        return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val isRightSide = offset.x > (size.width * 0.65f)
                        val isLeftSide = offset.x < (size.width * 0.35f)

                        if (isRightSide) {
                            onSeekRelative(10)
                            showForwardRipple = true
                            rippleHideJob?.cancel()
                            rippleHideJob = coroutineScope.launch { delay(400L.milliseconds); showForwardRipple = false }
                        } else if (isLeftSide) {
                            onSeekRelative(-10)
                            showRewindRipple = true
                            rippleHideJob?.cancel()
                            rippleHideJob = coroutineScope.launch { delay(400L.milliseconds); showRewindRipple = false }
                        }
                    }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        isDragVertical = false
                        isDragHorizontal = false
                        isIgnoredDrag = false
                        isLeftSideDrag = offset.x < (size.width * 0.5f)

                        val isOuterWidth = offset.x < (size.width * 0.20f) || offset.x > (size.width * 0.80f)
                        val isSafeHeight = offset.y > (size.height * 0.10f) && offset.y < (size.height * 0.90f)
                        isValidVerticalDragZone = isOuterWidth && isSafeHeight

                        accumulatedDx = 0f
                        accumulatedDy = 0f

                        // Snapshot hardware state at the exact moment the drag begins
                        initialSeekPosition = livePositionMs
                        initialVolume = deviceController.currentVolume
                        initialBrightness = deviceController.currentBrightness

                        scrubTargetPos = initialSeekPosition
                        lastSeekUpdate = System.currentTimeMillis()
                    },
                    onDrag = { change, dragAmount ->
                        if (isIgnoredDrag) return@detectDragGestures

                        change.consume()
                        accumulatedDx += dragAmount.x
                        accumulatedDy += dragAmount.y

                        if (!isDragVertical && !isDragHorizontal) {
                            val touchSlop = 40f
                            if (abs(accumulatedDx) > touchSlop || abs(accumulatedDy) > touchSlop) {
                                if (abs(accumulatedDx) > abs(accumulatedDy)) {
                                    isDragHorizontal = true
                                } else {
                                    if (isValidVerticalDragZone) {
                                        isDragVertical = true
                                    } else {
                                        isIgnoredDrag = true
                                        return@detectDragGestures
                                    }
                                }
                            }
                        }

                        if (isDragVertical) {
                            val deltaPercent = -accumulatedDy / (size.height * 0.5f)

                            if (isLeftSideDrag) {
                                val newBright = (initialBrightness + deltaPercent).coerceIn(0.01f, 1.0f)
                                deviceController.currentBrightness = newBright
                                showHud(HudType.BRIGHTNESS, newBright, "${(newBright * 100).toInt()}%")
                            } else {
                                val newVolExact = initialVolume + (deltaPercent * deviceController.maxVolume)
                                val newVolInt = newVolExact.toInt().coerceIn(0, deviceController.maxVolume)

                                deviceController.currentVolume = newVolInt

                                val volumePercent = newVolExact / deviceController.maxVolume
                                showHud(HudType.VOLUME, volumePercent, "${(volumePercent * 100).toInt()}%")
                            }
                        } else if (isDragHorizontal) {
                            val secondsPerPixel = 300.0 / size.width
                            val deltaMs = (accumulatedDx * secondsPerPixel * 1000).toLong()

                            scrubTargetPos = (initialSeekPosition + deltaMs).coerceIn(0L, liveDurationMs.coerceAtLeast(1L))

                            val progress = if (liveDurationMs > 0) scrubTargetPos.toFloat() / liveDurationMs.toFloat() else 0f
                            val text = "${formatDuration(scrubTargetPos)} / ${formatDuration(liveDurationMs)}"

                            showHud(HudType.SEEK, progress, text)

                            val now = System.currentTimeMillis()
                            if (now - lastSeekUpdate > 100L) {
                                lastSeekUpdate = now
                                onSeekScrub(scrubTargetPos)
                            }
                        }
                    },
                    onDragEnd = {
                        if (isDragHorizontal) onSeekScrubEnd(scrubTargetPos)
                        isDragVertical = false
                        isDragHorizontal = false
                        isIgnoredDrag = false
                    },
                    onDragCancel = {
                        isDragVertical = false
                        isDragHorizontal = false
                        isIgnoredDrag = false
                    }
                )
            }
    ) {
        DoubleTapSeekRipple(isForward = false, isVisible = showRewindRipple, modifier = Modifier.align(Alignment.CenterStart))
        DoubleTapSeekRipple(isForward = true, isVisible = showForwardRipple, modifier = Modifier.align(Alignment.CenterEnd))
        CenterHudOverlay(state = hudState, modifier = Modifier.align(Alignment.Center))
    }
}