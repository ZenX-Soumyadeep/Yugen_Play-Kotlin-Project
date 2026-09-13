package com.zenx.yugen.play.util

import android.net.Uri
import androidx.core.net.toUri

/**
 * CDN host repair for MegaPlay playlists.
 *
 * MegaPlay rotates the host it uses for HLS media segments. The playlists fetched from
 * `ncdn.imgnex.top` still advertise every segment on a `*.akirax.buzz` host, but those hosts
 * now answer HTTP 404 ("File with such name does not exist") / 403 for every segment. ExoPlayer
 * therefore burned through its retries and finally surfaced "No streams found".
 *
 * The exact same segment objects are served by [SEGMENT_CDN_HOST] (verified HTTP 200 across the
 * whole `seg-*` range with a `Referer: https://megaplay.buzz/` header), so segment requests are
 * transparently re-pointed there. Playlists, metadata and every other request are left untouched.
 */
object CdnHostRewriter {

    /** Live host that actually serves the media segments referenced by MegaPlay playlists. */
    const val SEGMENT_CDN_HOST = "fetch.nexabloom.top"

    /** Suffix shared by the stale segment hosts that MegaPlay playlists still reference. */
    private const val STALE_SEGMENT_HOST_SUFFIX = "akirax.buzz"

    /**
     * Returns [uri] unchanged, or an equivalent URI whose host points at [SEGMENT_CDN_HOST] when
     * [uri] targets a stale `*.akirax.buzz` segment host.
     */
    fun rewriteSegmentHost(uri: Uri): Uri {
        val host = uri.host.orEmpty()
        if (!host.endsWith(STALE_SEGMENT_HOST_SUFFIX, ignoreCase = true)) return uri
        return uri.buildUpon().authority(SEGMENT_CDN_HOST).build()
    }

    /** String overload used when rewriting playlist bodies (e.g. the cast relay). */
    fun rewriteSegmentHost(url: String): String = try {
        rewriteSegmentHost(url.toUri()).toString()
    } catch (e: Exception) {
        android.util.Log.w("CdnHostRewriter", "Failed to rewrite segment host for $url", e)
        url
    }
}
