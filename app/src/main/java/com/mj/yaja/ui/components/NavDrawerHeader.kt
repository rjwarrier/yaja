package com.mj.yaja.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mj.yaja.R
import com.mj.yaja.ui.theme.BodoniModaFamily

@Composable
internal fun DrawerBrandHeader(
        versionName: String,
        onLogoClick: () -> Unit,
        onOpenPlayStore: () -> Unit,
        onOpenWebsite: () -> Unit,
        onShareApp: () -> Unit
) {
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier =
                                Modifier.size(52.dp).clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onLogoClick
                                )
                ) {
                        Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(
                                        painter = painterResource(id = R.drawable.rj_logo),
                                        contentDescription = stringResource(R.string.nav_cd_rj_logo),
                                        modifier = Modifier.size(52.dp).padding(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                        }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                        "yaja",
                        style =
                                MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = BodoniModaFamily,
                                        fontWeight = FontWeight.ExtraBold
                                ),
                        color = MaterialTheme.colorScheme.primary
                )
                Text(
                        stringResource(R.string.nav_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                                modifier = Modifier.height(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = stringResource(R.string.nav_version_format, versionName),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                        )
                                }
                        }
                        DrawerHeaderIconButton(
                                icon = Icons.Rounded.Shop,
                                contentDescription = stringResource(R.string.nav_cd_play_store),
                                onClick = onOpenPlayStore
                        )
                        DrawerHeaderIconButton(
                                icon = Icons.Rounded.Language,
                                contentDescription = stringResource(R.string.nav_cd_website),
                                onClick = onOpenWebsite
                        )
                        DrawerHeaderIconButton(
                                icon = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.nav_cd_share_app),
                                onClick = onShareApp
                        )
                }
        }
}
