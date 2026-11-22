package com.nexus.core.storage

import java.net.URLDecoder
import java.net.URLEncoder

/** URLs of the Supabase Storage REST API for one bucket. */
internal class SupabasePaths(projectUrl: String, private val bucket: String) {

    private val base = projectUrl.trim().trimEnd('/')

    val isConfigured: Boolean = base.startsWith("https://")

    /** Endpoint to upload or delete the object (authenticated). */
    fun objectUrl(path: String) = "$base/storage/v1/object/$bucket/${encode(path)}"

    /** URL anyone can download from; the bucket is public for reads only. */
    fun publicUrl(path: String) = "$base/storage/v1/object/public/$bucket/${encode(path)}"

    /** The path inside the bucket of a URL made by [publicUrl], or null for any other URL. */
    fun pathOf(url: String): String? {
        val prefix = "$base/storage/v1/object/public/$bucket/"
        if (!isConfigured || !url.startsWith(prefix)) return null
        return url.removePrefix(prefix).substringBefore('?').split('/')
            .joinToString("/") { URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") }
            .takeIf { it.isNotBlank() }
    }

    // Each segment is percent-encoded on its own: the slashes must stay folder separators
    private fun encode(path: String) = path.trim('/').split('/').joinToString("/") { segment ->
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
    }
}
