package com.papercut.collage.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.papercut.collage.R
import com.papercut.collage.model.BoardAspect
import com.papercut.collage.model.BoardBackground
import androidx.compose.ui.text.font.FontFamily
import com.papercut.collage.model.ClockOverlay
import com.papercut.collage.model.ClockStyle
import com.papercut.collage.model.GradientPreset
import com.papercut.collage.model.OverlayFont
import com.papercut.collage.model.TextureKind

/**
 * Board settings: shape and background (#4, #5). Swatches are rendered by the
 * same code that paints the real board, so what you tap is what you get.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardSheet(
    aspect: BoardAspect,
    background: BoardBackground,
    cornerRadius: Float,
    clock: ClockOverlay,
    onAspect: (BoardAspect) -> Unit,
    onBackground: (BoardBackground) -> Unit,
    onCornerRadius: (Float) -> Unit,
    onClock: (ClockOverlay) -> Unit,
    onPickImage: (android.net.Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pickBackground = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onPickImage(uri) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.board_shape), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BoardAspect.entries.forEach { option ->
                    FilterChip(
                        selected = option == aspect,
                        onClick = { onAspect(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            // Rounding only shows on a background, so don't offer a slider that
            // visibly does nothing on a transparent board.
            if (background !is BoardBackground.None) {
                Text(
                    stringResource(R.string.board_corner_radius),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Slider(
                    value = cornerRadius,
                    onValueChange = onCornerRadius,
                    valueRange = 0f..1f,
                )
            }

            Text(
                stringResource(R.string.board_background),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Transparent stays first — it's the signature look, not a fallback.
                Swatch(
                    selected = background is BoardBackground.None,
                    label = stringResource(R.string.background_none),
                    onClick = { onBackground(BoardBackground.None) },
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) { Text("∅", style = MaterialTheme.typography.titleLarge) }
                }

                Swatch(
                    selected = background is BoardBackground.Image,
                    label = stringResource(R.string.background_gallery),
                    onClick = {
                        pickBackground.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) }
                }

                GradientPreset.entries.forEach { preset ->
                    val isSelected =
                        background is BoardBackground.Gradient && background.preset == preset
                    Swatch(
                        selected = isSelected,
                        label = preset.label,
                        onClick = { onBackground(BoardBackground.Gradient(preset)) },
                    ) {
                        BackgroundPreview(
                            background = BoardBackground.Gradient(preset),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }

                TextureKind.entries.forEach { kind ->
                    val isSelected =
                        background is BoardBackground.Texture && background.kind == kind
                    Swatch(
                        selected = isSelected,
                        label = kind.label,
                        onClick = { onBackground(BoardBackground.Texture(kind)) },
                    ) {
                        BackgroundPreview(
                            background = BoardBackground.Texture(kind),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }

            ClockSection(clock = clock, onClock = onClock)
        }
    }
}

/**
 * Clock settings. The widget ticks this natively via TextClock, so it costs
 * nothing to leave on.
 */
@Composable
private fun ClockSection(clock: ClockOverlay, onClock: (ClockOverlay) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.clock), style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = clock.enabled,
            onCheckedChange = { onClock(clock.copy(enabled = it)) },
        )
    }

    if (!clock.enabled) return

    Text(stringResource(R.string.clock_format), style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClockStyle.entries.forEach { style ->
            FilterChip(
                selected = style == clock.style,
                onClick = { onClock(clock.copy(style = style)) },
                label = { Text(style.label) },
            )
        }
    }

    Text(stringResource(R.string.clock_font), style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverlayFont.entries.forEach { font ->
            FilterChip(
                selected = font == clock.font,
                onClick = { onClock(clock.copy(font = font)) },
                label = { Text(font.label, fontFamily = FontFamily(font.typeface())) },
            )
        }
    }

    // Position is no longer a preset — the clock is dragged on the board.
    Text(
        stringResource(R.string.clock_drag_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(stringResource(R.string.clock_colour), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OVERLAY_COLORS.forEach { colorValue ->
            val selected = colorValue == clock.color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(colorValue))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onClock(clock.copy(color = colorValue)) },
            )
        }
    }

    Text(stringResource(R.string.clock_size), style = MaterialTheme.typography.labelLarge)
    Slider(
        value = clock.sizeFraction,
        onValueChange = { onClock(clock.copy(sizeFraction = it)) },
        valueRange = 0.05f..0.5f,
    )
}

@Composable
private fun Swatch(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .then(
                    if (selected) {
                        Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.medium,
                        )
                    } else {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.medium,
                        )
                    },
                ),
        ) { content() }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
    }
}
