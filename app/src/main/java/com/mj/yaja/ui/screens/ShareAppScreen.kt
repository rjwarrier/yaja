package com.mj.yaja.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.mj.yaja.R
import com.mj.yaja.ui.design.AppScreenReveal
import com.mj.yaja.ui.design.expressivePressMotion
import java.io.File

private const val SHARE_LINK = "https://play.google.com/store/apps/details?id=com.mj.yaja"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val defaultMessage = stringResource(R.string.share_app_default_message)
    var message by remember { mutableStateOf(defaultMessage) }
    var includeImage by remember { mutableStateOf(true) }
    val chooserTitle = stringResource(R.string.nav_share_yaja)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.share_app_title),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AppScreenReveal(visible = true, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (includeImage) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.share_promo),
                            contentDescription = stringResource(R.string.share_app_image_cd),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.share_app_include_image_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.share_app_include_image_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeImage,
                            onCheckedChange = { includeImage = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.share_app_message_label)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.share_app_link_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SHARE_LINK,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(20.dp))

                val interactionShare = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        shareApp(
                            context = context,
                            message = message,
                            includeImage = includeImage,
                            chooserTitle = chooserTitle
                        )
                    },
                    interactionSource = interactionShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .expressivePressMotion(interactionShare, pressedScale = 0.97f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.nav_share_yaja))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun shareApp(
    context: Context,
    message: String,
    includeImage: Boolean,
    chooserTitle: String
) {
    val body = buildString {
        val trimmed = message.trim()
        if (trimmed.isNotEmpty()) {
            append(trimmed)
            append("\n\n")
        }
        append(SHARE_LINK)
    }

    val imageUri = if (includeImage) stagePromoImage(context) else null
    val intent = Intent(Intent.ACTION_SEND).apply {
        // image/jpeg (not text/plain) is what makes share targets treat this as an
        // image-with-caption rather than dropping the attachment silently.
        type = if (imageUri != null) "image/jpeg" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
        if (imageUri != null) {
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/** Copy the promo drawable into cache/exports/ so FileProvider can hand it to the chooser. */
private fun stagePromoImage(context: Context): Uri? = runCatching {
    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val imageFile = File(exportsDir, "share_promo.jpg")
    context.resources.openRawResource(R.drawable.share_promo).use { input ->
        imageFile.outputStream().use { output -> input.copyTo(output) }
    }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}.getOrNull()
