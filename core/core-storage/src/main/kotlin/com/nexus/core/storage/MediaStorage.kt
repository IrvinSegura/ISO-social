package com.nexus.core.storage

import android.net.Uri
import java.io.IOException

/**
 * Where user media (post photos, voice notes, attachments, eco and story media) lives.
 * Features only see this interface, so the provider can change without touching them.
 */
interface MediaStorage {

    /**
     * Uploads [content] to [path] and returns its public URL.
     * A path is written once: uploading to an existing path fails instead of overwriting.
     */
    suspend fun upload(path: String, content: MediaContent): String

    /** Deletes the file at [path]; used to clean up after a failed upload or a deleted post. */
    suspend fun delete(path: String)

    /** The path of a URL returned by [upload], or null when the URL is not one of ours. */
    fun pathOf(publicUrl: String): String?
}

sealed interface MediaContent {

    /** Bytes already in memory, e.g. a compressed photo. */
    class Bytes(val bytes: ByteArray, val contentType: String) : MediaContent

    /**
     * A content:// or file:// file, streamed from disk so a 50 MB video never sits in memory.
     * [contentType] is used when the system cannot tell it (file:// URIs have none).
     */
    data class Local(val uri: Uri, val contentType: String? = null) : MediaContent
}

class MediaStorageException(message: String, cause: Throwable? = null) : IOException(message, cause)
