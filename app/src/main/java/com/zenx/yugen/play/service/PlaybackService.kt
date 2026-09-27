package com.zenx.yugen.play.service

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zenx.yugen.play.ui.player.PlayerViewModel

class PlaybackService : MediaSessionService() {
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return PlayerViewModel.activeMediaSession
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val player = PlayerViewModel.activeMediaSession?.player
        if (player?.playWhenReady == true) {
            player.pause()
        }
        stopSelf()
    }
}
