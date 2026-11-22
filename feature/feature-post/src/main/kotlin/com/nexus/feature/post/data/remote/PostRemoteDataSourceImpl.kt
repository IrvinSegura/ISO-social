package com.nexus.feature.post.data.remote

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nexus.core.common.media.ImageCompressor
import com.nexus.core.storage.MediaContent
import com.nexus.core.storage.MediaStorage
import com.nexus.feature.post.data.model.EcoDto
import com.nexus.feature.post.data.model.PostDto
import com.nexus.feature.post.domain.model.EcoType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class PostRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaStorage: MediaStorage,
    private val imageCompressor: ImageCompressor,
) : PostRemoteDataSource {

    override suspend fun getPost(postId: String): PostDto {
        val doc = firestore.collection(POSTS).document(postId).get().await()
        return doc.toObject(PostDto::class.java)?.copy(id = doc.id)
            ?: error("Post $postId not found")
    }

    override suspend fun getEcos(postId: String): List<EcoDto> {
        return firestore.collection(POSTS).document(postId)
            .collection(ECOS)
            // The newest MAX_ECOS only: a post with 2,000 ecos would otherwise cost 2,000
            // reads every time it is opened. Read newest first, shown oldest first
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(MAX_ECOS)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(EcoDto::class.java)?.copy(id = doc.id)
            }
            .asReversed()
    }

    override suspend fun createPost(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String?,
        content: String,
        circleId: String?,
        circleName: String?,
        imageUris: List<Uri>,
        audioPath: String?,
        fileUri: Uri?,
        fileName: String?,
        mood: String?,
        locationName: String?,
        gifUrl: String?,
    ): String {
        val postId = firestore.collection(POSTS).document().id
        // Inside the author's folder: the bucket policies only let a user write under their uid
        val folder = "$POSTS/$authorId/$postId"
        val uploadedPaths = mutableListOf<String>()

        try {
            // Media uploads are independent, so they run in parallel
            val (imageUrls, audioUrl, uploadedFileUrl) = coroutineScope {
                val images = imageUris.mapIndexed { index, uri ->
                    async { uploadImage("$folder/images/image_$index.jpg", uri, uploadedPaths) }
                }
                val audio = audioPath?.let { path ->
                    async {
                        // file:// URIs have no MIME type, and the bucket only accepts known types
                        upload("$folder/audio.m4a", MediaContent.Local(Uri.fromFile(File(path)), AUDIO_TYPE), uploadedPaths)
                    }
                }
                val file = fileUri?.let { uri ->
                    async {
                        upload("$folder/files/${sanitizeFileName(fileName)}", MediaContent.Local(uri), uploadedPaths)
                    }
                }
                Triple(images.awaitAll(), audio?.await(), file?.await())
            }

            val now = System.currentTimeMillis()
            val data = mapOf(
                "authorId" to authorId,
                "authorName" to authorName,
                "authorPhotoUrl" to authorPhotoUrl,
                "content" to content,
                "circleId" to circleId,
                "circleName" to circleName,
                "imageUrls" to imageUrls,
                "audioUrl" to audioUrl,
                "fileUrl" to uploadedFileUrl,
                "fileName" to fileName,
                "mood" to mood,
                "locationName" to locationName,
                "gifUrl" to gifUrl,
                "ecoCount" to 0,
                "reactions" to emptyMap<String, Long>(),
                "createdAt" to now,
                "updatedAt" to now,
            )
            firestore.collection(POSTS).document(postId).set(data).await()
            return postId
        } catch (e: Exception) {
            // Do not leave orphaned files in storage when the post could not be created
            deleteQuietly(synchronized(uploadedPaths) { uploadedPaths.toList() })
            throw e
        }
    }

    private suspend fun upload(
        path: String,
        content: MediaContent,
        uploadedPaths: MutableList<String>,
    ): String {
        val url = mediaStorage.upload(path, content)
        synchronized(uploadedPaths) { uploadedPaths += path }
        return url
    }

    private suspend fun uploadImage(path: String, uri: Uri, uploadedPaths: MutableList<String>): String =
        upload(path, imageContent(uri), uploadedPaths)

    // Photos go up compressed; GIFs and anything the compressor cannot decode go up as is
    private suspend fun imageContent(uri: Uri): MediaContent =
        imageCompressor.compress(uri)
            ?.let { MediaContent.Bytes(it, JPEG_TYPE) }
            ?: MediaContent.Local(uri)

    private suspend fun deleteQuietly(paths: List<String>) = withContext(NonCancellable) {
        paths.forEach { path -> runCatching { mediaStorage.delete(path) } }
    }

    // The name comes from the picked document, so it must not be able to escape the post folder
    private fun sanitizeFileName(name: String?): String {
        val cleaned = name.orEmpty()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim('.', ' ')
            .take(MAX_FILE_NAME_LENGTH)
        return cleaned.ifBlank { "file" }
    }

    override suspend fun sendEco(
        postId: String,
        authorId: String,
        authorName: String,
        type: EcoType,
        message: String,
        emoji: String?,
        mediaUrl: String?,
        parentId: String?,
        replyToName: String?,
    ) {
        // Image and audio ecos arrive as a local content:// or file:// URI that only this
        // device can open, so the file is uploaded first and the eco stores its public URL
        val localUri = mediaUrl
            ?.takeIf { type == EcoType.IMAGE || type == EcoType.AUDIO }
            ?.let(Uri::parse)
            ?.takeIf { it.scheme == "content" || it.scheme == "file" }
        val ext = if (type == EcoType.AUDIO) "m4a" else "jpg"
        val uploadPath = localUri?.let { "$ECOS/$postId/$authorId/${UUID.randomUUID()}.$ext" }
        val uploadedUrl = if (localUri != null && uploadPath != null) {
            val content = if (type == EcoType.IMAGE) imageContent(localUri)
            else MediaContent.Local(localUri, AUDIO_TYPE)
            mediaStorage.upload(uploadPath, content)
        } else null

        try {
            val ecoData = mutableMapOf<String, Any?>(
                "authorId" to authorId,
                "authorName" to authorName,
                "type" to type.name,
                "message" to message,
                "emoji" to emoji,
                "mediaUrl" to (uploadedUrl ?: mediaUrl),
                "createdAt" to System.currentTimeMillis(),
            )
            // A reply joins its thread; the rules check the parent is a top-level eco
            if (parentId != null) {
                ecoData["parentId"] = parentId
                ecoData["replyToName"] = replyToName
            }
            // Batch: the eco and its counter are written together or not at all.
            // lastEcoId lets the security rules check that the +1 belongs to a real new eco
            val postRef = firestore.collection(POSTS).document(postId)
            val ecoRef = postRef.collection(ECOS).document()
            firestore.batch()
                .set(ecoRef, ecoData)
                .update(postRef, mapOf("ecoCount" to FieldValue.increment(1), "lastEcoId" to ecoRef.id))
                .commit()
                .await()
        } catch (e: Exception) {
            // Do not leave an orphaned file in storage when the eco could not be saved
            if (uploadedUrl != null && uploadPath != null) deleteQuietly(listOf(uploadPath))
            throw e
        }
    }

    override suspend fun setEcoLiked(postId: String, ecoId: String, userId: String, liked: Boolean) {
        // arrayUnion/arrayRemove only touch this user's entry, which is all the rules allow
        val change = if (liked) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId)
        firestore.collection(POSTS).document(postId).collection(ECOS).document(ecoId)
            .update(LIKED_BY, change)
            .await()
    }

    override suspend fun updatePost(postId: String, content: String, mood: String?) {
        firestore.collection(POSTS).document(postId).update(
            mapOf(
                "content" to content,
                "mood" to mood,
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    override suspend fun deletePost(postId: String, authorId: String) {
        val ref = firestore.collection(POSTS).document(postId)
        val post = ref.get().await().toObject(PostDto::class.java)
        ref.delete().await()
        // The post is gone for everyone already; its files are only cleaned up when they sit
        // in this post's folder, so a crafted URL can never point the delete elsewhere
        val folder = "$POSTS/$authorId/$postId/"
        val mediaPaths = listOfNotNull(post?.audioUrl, post?.fileUrl)
            .plus(post?.imageUrls.orEmpty())
            .mapNotNull(mediaStorage::pathOf)
            .filter { it.startsWith(folder) }
        deleteQuietly(mediaPaths)
    }

    companion object {
        private const val POSTS = "posts"
        private const val ECOS = "ecos"
        private const val MAX_FILE_NAME_LENGTH = 100
        // Enough for a long conversation; replies count too, since threads share the list
        private const val MAX_ECOS = 100L
        private const val LIKED_BY = "likedBy"
        private const val JPEG_TYPE = "image/jpeg"
        private const val AUDIO_TYPE = "audio/mp4"
    }
}
