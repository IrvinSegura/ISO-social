package com.nexus.core.ui.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * "Compartir": the popular apps installed on the phone with their own icons, copying the
 * text, and the system sheet for anything else. Sending prepares the image first, so the
 * tapped app shows a spinner meanwhile.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareSheet(post: SharedPost, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targets = remember { PostShare.installedTargets(context) }
    var preparing by remember { mutableStateOf<String?>(null) }

    fun send(packageName: String?) {
        if (preparing != null) return
        preparing = packageName ?: MORE
        scope.launch {
            try {
                val intent = PostShare.intent(context, post, packageName)
                context.startActivity(if (packageName == null) PostShare.chooser(intent) else intent)
                onDismiss()
            } catch (e: ActivityNotFoundException) {
                Timber.w(e, "Share target is gone")
                context.startActivity(PostShare.chooser(PostShare.intent(context, post, null)))
                onDismiss()
            } finally {
                preparing = null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text("Compartir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "Se envía el texto con la foto de la publicación, o una tarjeta si no tiene.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 4,
                modifier = Modifier.fillMaxWidth(),
            ) {
                targets.forEach { target ->
                    ShareTile(
                        label = target.label,
                        loading = preparing == target.packageName,
                        onClick = { send(target.packageName) },
                    ) {
                        val icon = remember(target.packageName) { target.icon.toBitmap(144, 144).asImageBitmap() }
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
                    }
                }
                ShareTile(label = "Copiar texto", loading = false, onClick = {
                    copy(context, PostShare.text(post))
                    onDismiss()
                }) { RoundIcon(Icons.Outlined.ContentCopy) }
                ShareTile(label = "Más", loading = preparing == MORE, onClick = { send(null) }) {
                    RoundIcon(Icons.Outlined.MoreHoriz)
                }
            }
        }
    }
}

private const val MORE = "more"

@Composable
private fun ShareTile(
    label: String,
    loading: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
            icon()
            if (loading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RoundIcon(icon: ImageVector) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Publicación", text))
    Toast.makeText(context, "Texto copiado", Toast.LENGTH_SHORT).show()
}
