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
    const val SEGMENT_CDN_HOST = "ncdn.imgnex.top"

    /** Suffix shared by the stale segment hosts that MegaPlay playlists still reference. */
    private const val STALE_SEGMENT_HOST_SUFFIX = "akirax.buzz"

    /**
     * Returns [uri] unchanged.
     *
     * Note: MegaPlay streams segment files via its authoritative playlist host (e.g. `*.akirax.buzz`),
     * which actively serves media segments with HTTP 200. Rewriting to `ncdn.imgnex.top` caused HTTP 404
     * playback failures because `ncdn.imgnex.top` only hosts playlists, not the segment binaries.
     */
    fun rewriteSegmentHost(uri: Uri): Uri {
        return uri
    }

    /** String overload used when rewriting playlist bodies (e.g. the cast relay). */
    fun rewriteSegmentHost(url: String): String = url
}
