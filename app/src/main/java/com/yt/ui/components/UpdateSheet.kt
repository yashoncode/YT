package com.yt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yt.R
import com.yt.ui.components.shared.rememberYTSheetState
import com.yt.ui.screens.update.UpdateUiState

private val NotesMaxHeight = 240.dp

/**
 * The update popup: what is new and a way to get it, or the answer to a manual check.
 * Renders nothing while there is nothing to say.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onUpdate: (downloadUrl: String) -> Unit,
) {
    if (state.release == null && !state.upToDate && !state.failed) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberYTSheetState(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val release = state.release
            when {
                release != null -> {
                    Text(
                        text = stringResource(R.string.update_sheet_title, release.version),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = NotesMaxHeight)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        if (release.changelog.isBlank()) {
                            Text(
                                text = stringResource(R.string.update_sheet_notes_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            MarkdownChangelogText(markdown = release.changelog)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.maybe_later))
                        }
                        Button(
                            onClick = { onUpdate(release.downloadUrl) },
                            modifier = Modifier.weight(1.4f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Text(
                                text = stringResource(R.string.update_yt),
                                modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.update_sheet_footnote),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.upToDate -> {
                    UpdateCheckOutcome(
                        icon = Icons.Outlined.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        text = stringResource(R.string.yt_is_up_to_date),
                        onClose = onDismiss,
                    )
                }

                else -> {
                    UpdateCheckOutcome(
                        icon = Icons.Outlined.ErrorOutline,
                        tint = MaterialTheme.colorScheme.error,
                        text = stringResource(R.string.update_check_failed),
                        onClose = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateCheckOutcome(
    icon: ImageVector,
    tint: Color,
    text: String,
    onClose: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
    FilledTonalButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.close))
    }
}

/** Release notes as GitHub writes them: headings, `-`/`*` bullets and `**bold**`. */
@Composable
private fun MarkdownChangelogText(markdown: String) {
    val styled = remember(markdown) { parseChangelog(markdown) }
    Text(
        text = styled,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun parseChangelog(markdown: String): AnnotatedString =
    buildAnnotatedString {
        markdown.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { index, line ->
            if (index > 0) append("\n")
            when {
                line.startsWith("#") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line.trimStart('#').trim()) }
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    append("• ")
                    appendBold(line.substring(2).trim())
                }

                else -> {
                    appendBold(line)
                }
            }
        }
    }

private fun AnnotatedString.Builder.appendBold(text: String) {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}
