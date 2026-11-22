package com.nexus.core.storage

import android.content.Context
import android.provider.OpenableColumns
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [MediaStorage] on Supabase Storage. Requests carry the user's Firebase ID token: the
 * project trusts Firebase Auth (third-party auth) and the bucket policies check the token's
 * issuer and uid against the folder (supabase/storage-setup.sql).
 */
@Singleton
class SupabaseMediaStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
) : MediaStorage {

    private val paths = SupabasePaths(BuildConfig.SUPABASE_URL, BUCKET)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Story videos can take a while to go up on a slow connection
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .build()

    override suspend fun upload(path: String, content: MediaContent): String {
        val body = when (content) {
            is MediaContent.Bytes -> content.bytes.toRequestBody(content.contentType.toMediaTypeOrNull())
            is MediaContent.Local -> localBody(content)
        }
        val request = authorizedRequest(path)
            // Paths are unique (post id, UUID): never replace a file that already exists
            .header("x-upsert", "false")
            // Files never change after upload, so they can be cached for a long time
            .header("cache-control", "max-age=31536000")
            .post(body)
            .build()
        execute(request, "upload $path")
        return paths.publicUrl(path)
    }

    override suspend fun delete(path: String) {
        execute(authorizedRequest(path).delete().build(), "delete $path")
    }

    override fun pathOf(publicUrl: String): String? = paths.pathOf(publicUrl)

    private suspend fun authorizedRequest(path: String): Request.Builder {
        if (!paths.isConfigured || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            throw MediaStorageException("El almacenamiento de archivos no está configurado")
        }
        val token = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw MediaStorageException("Inicia sesión para subir archivos")
        return Request.Builder()
            .url(paths.objectUrl(path))
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
    }

    // Streams the file from disk instead of reading it into memory
    private fun localBody(content: MediaContent.Local): RequestBody {
        val resolver = context.contentResolver
        val type = (resolver.getType(content.uri) ?: content.contentType ?: "application/octet-stream")
            .toMediaTypeOrNull()
        val length = sizeOf(content)
        return object : RequestBody() {
            override fun contentType(): MediaType? = type
            override fun contentLength(): Long = length
            override fun writeTo(sink: BufferedSink) {
                val input = resolver.openInputStream(content.uri)
                    ?: throw IOException("No se pudo abrir ${content.uri}")
                input.source().use { sink.writeAll(it) }
            }
        }
    }

    private fun sizeOf(content: MediaContent.Local): Long = runCatching {
        context.contentResolver.query(content.uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null }
            ?: context.contentResolver.openAssetFileDescriptor(content.uri, "r")?.use { it.length }
    }.getOrNull()?.takeIf { it >= 0 } ?: -1L

    // enqueue instead of execute: cancelling the coroutine (screen closed, timeout) cancels
    // the HTTP call too, instead of leaving it running on a blocked thread
    private suspend fun execute(request: Request, action: String) {
        val response = suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) = continuation.resume(response)
                override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            })
        }
        response.use {
            if (it.isSuccessful) return
            val detail = it.body?.string().orEmpty().take(300)
            Timber.w("Supabase could not $action: ${it.code} $detail")
            throw MediaStorageException(
                when (it.code) {
                    401, 403 -> "No tienes permiso para subir este archivo"
                    409 -> "El archivo ya existe"
                    413 -> "El archivo es demasiado grande"
                    415 -> "Ese tipo de archivo no está permitido"
                    else -> "No se pudo subir el archivo (${it.code})"
                },
            )
        }
    }

    private companion object {
        const val BUCKET = "media"
    }
}
