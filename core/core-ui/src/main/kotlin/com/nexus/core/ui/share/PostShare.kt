package com.nexus.core.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What gets shared from a post. Only public posts are shared; a circle's posts stay in it. */
data class SharedPost(
    val id: String,
    val content: String,
    val authorName: String,
    val mood: String? = null,
    /** The first photo or the GIF; without one, a card with the text is drawn instead. */
    val imageUrl: String? = null,
)

/** An app the post can go to, with the icon the device shows for it. */
data class ShareTarget(val packageName: String, val label: String, val icon: Drawable)

object PostShare {

    /** The best-known apps, in the order the sheet shows them; only installed ones appear. */
    val POPULAR_APPS = listOf(
        "com.whatsapp" to "WhatsApp",
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.twitter.android" to "X",
        "org.telegram.messenger" to "Telegram",
        "com.facebook.orca" to "Messenger",
        "com.snapchat.android" to "Snapchat",
        "com.google.android.gm" to "Gmail",
    )

    private const val APP_NAME = "Nexus"
    private const val SHARE_DIR = "shared"
    private const val MAX_EXCERPT = 280

    fun installedTargets(context: Context): List<ShareTarget> {
        val pm = context.packageManager
        return POPULAR_APPS.mapNotNull { (pkg, label) ->
            try {
                ShareTarget(pkg, label, pm.getApplicationIcon(pkg))
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    fun text(post: SharedPost): String {
        val excerpt = post.content.trim().let {
            if (it.length > MAX_EXCERPT) it.take(MAX_EXCERPT).trimEnd() + "…" else it
        }
        return buildString {
            if (excerpt.isNotEmpty()) append("“").append(excerpt).append("”\n\n")
            append("— ").append(post.authorName).append(" en ").append(APP_NAME)
        }
    }

    /**
     * Builds the send intent: the text plus an image (the post's photo, or a drawn card so
     * apps that only take pictures, like Instagram, still get something). [packageName]
     * sends it straight to that app; null leaves the choice to the system sheet.
     */
    suspend fun intent(context: Context, post: SharedPost, packageName: String?): Intent {
        val image = runCatching { imageFile(context, post) }.getOrNull()
        val uri = image?.let {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = if (uri != null) "image/jpeg" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, text(post))
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                // The receiving app needs the grant for the file, and some read it from the clip
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            packageName?.let(::setPackage)
        }
    }

    fun chooser(intent: Intent): Intent =
        Intent.createChooser(intent, "Compartir publicación").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    private suspend fun imageFile(context: Context, post: SharedPost): File {
        val bitmap = post.imageUrl?.let { loadBitmap(context, it) } ?: drawCard(post)
        return withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
            // One file per post, replaced on every share: the folder never grows
            File(dir, "post_${post.id.filter(Char::isLetterOrDigit)}.jpg").also { file ->
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
        }
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
        return result.drawable.toBitmap()
    }

    /** A 4:5 card in the app's style: dark background, amber mood pill, the text and the author. */
    internal fun drawCard(post: SharedPost): Bitmap {
        val width = 1080
        val height = 1350
        val margin = 96f
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val background = Paint().apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xFF111827.toInt(), 0xFF030712.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
        // A soft amber glow in the corner, like the app's accent
        val glow = Paint().apply {
            shader = android.graphics.RadialGradient(width.toFloat(), 0f, 700f, 0x55F59E0B, 0x00F59E0B, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)

        var y = margin + 40f
        post.mood?.takeIf { it.isNotBlank() }?.let { mood ->
            val pillText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF111827.toInt()
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textWidth = pillText.measureText(mood)
            val pill = RectF(margin, y, margin + textWidth + 56f, y + 72f)
            canvas.drawRoundRect(pill, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFBBF24.toInt() })
            canvas.drawText(mood, margin + 28f, y + 50f, pillText)
            y += 120f
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            // Short posts read like a quote; long ones shrink so more of them fits
            textSize = when {
                post.content.length <= 60 -> 96f
                post.content.length <= 160 -> 70f
                else -> 54f
            }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = (width - margin * 2).toInt()
        val maxLines = ((height - y - 300f) / (bodyPaint.textSize * 1.25f)).toInt().coerceAtLeast(1)
        val body = StaticLayout.Builder.obtain(post.content.trim(), 0, post.content.trim().length, bodyPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(margin, y)
        body.draw(canvas)
        canvas.restore()

        val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xB3FFFFFF.toInt()
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(post.authorName, margin, height - margin - 70f, footer.apply { color = 0xFFFFFFFF.toInt() })
        canvas.drawText(
            APP_NAME,
            margin,
            height - margin,
            footer.apply {
                color = 0xFFFBBF24.toInt()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        return bitmap
    }
}
