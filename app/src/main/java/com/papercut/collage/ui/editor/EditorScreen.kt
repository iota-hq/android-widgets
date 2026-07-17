package com.papercut.collage.ui.editor

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.papercut.collage.R
import com.papercut.collage.model.Collage
import com.papercut.collage.model.CollagePiece
import com.papercut.collage.model.OverlayFont
import com.papercut.collage.model.TextPiece
import com.papercut.collage.render.ClockRenderer
import com.papercut.collage.render.TextRenderer
import java.io.File
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/** Photo Picker cap for one batch — enough for a dense collage, bounded work. */
private const val MAX_PHOTOS_PER_PICK = 10

/** Colours offered for the clock and text overlays. */
internal val OVERLAY_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF1A1A1A.toInt(),
    0xFFF6D365.toInt(),
    0xFFFF6B6B.toInt(),
    0xFF4ECDC4.toInt(),
    0xFFB388FF.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    collageId: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pinUnsupported = stringResource(R.string.pin_unsupported)
    var showBoardSheet by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }
    var showAddToHomeInfo by remember { mutableStateOf(false) }
    // Non-null = text dialog open; empty string id = adding a new text.
    var editingTextId by remember { mutableStateOf<String?>(null) }

    // Set expectations before the launcher gets involved. On MIUI the widget is
    // routinely placed without being drawn, and a user with no warning just sees
    // a button that did nothing.
    if (showAddToHomeInfo) {
        AlertDialog(
            onDismissRequest = { showAddToHomeInfo = false },
            title = { Text(stringResource(R.string.add_to_home_title)) },
            text = { Text(stringResource(R.string.add_to_home_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showAddToHomeInfo = false
                    viewModel.addToHomeScreen(
                        onUnsupported = {
                            scope.launch { snackbarHost.showSnackbar(pinUnsupported) }
                        },
                    )
                }) { Text(stringResource(R.string.understood)) }
            },
        )
    }

    LaunchedEffect(collageId) { viewModel.load(collageId) }

    // Surface batch failures without stealing the screen.
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Multi-select (#9): the whole selection is processed, one photo at a time.
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS_PER_PICK),
    ) { uris -> viewModel.addPhotos(uris) }

    if (showBoardSheet) {
        BoardSheet(
            aspect = state.collage.aspect,
            background = state.collage.background,
            cornerRadius = state.collage.cornerRadius,
            clock = state.collage.clock,
            onAspect = viewModel::setAspect,
            onBackground = viewModel::setBackground,
            onCornerRadius = viewModel::setCornerRadius,
            onClock = viewModel::setClock,
            onPickImage = viewModel::setBackgroundFromGallery,
            onDismiss = { showBoardSheet = false },
        )
    }

    if (showLayersSheet) {
        LayersSheet(
            collage = state.collage,
            selectedId = state.selectedId,
            onSelect = { id ->
                viewModel.select(id)
                showLayersSheet = false
            },
            onMove = viewModel::moveLayer,
            onDelete = viewModel::delete,
            onDismiss = { showLayersSheet = false },
        )
    }

    editingTextId?.let { id ->
        val existing = state.collage.texts.find { it.id == id }
        TextEntryDialog(
            initial = existing?.text.orEmpty(),
            onConfirm = { value ->
                if (existing == null) viewModel.addText(value)
                else viewModel.updateText(id) { it.copy(text = value.trim()) }
                editingTextId = null
            },
            onDismiss = { editingTextId = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // No title text: with four actions there's no room, and it was
                // wrapping to a broken "E di…". The board on screen is context.
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { editingTextId = "" }) {
                        Icon(
                            Icons.Filled.TextFields,
                            contentDescription = stringResource(R.string.add_text),
                        )
                    }
                    IconButton(onClick = { showLayersSheet = true }) {
                        Icon(
                            Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.layers),
                        )
                    }
                    IconButton(onClick = { showBoardSheet = true }) {
                        Icon(
                            Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.board_settings),
                        )
                    }
                    // A labelled button, not an icon: "add this to my home
                    // screen" is the whole point of the app and shouldn't be a
                    // guess.
                    Button(
                        enabled = state.collage.pieces.isNotEmpty() || state.collage.texts.isNotEmpty(),
                        onClick = { showAddToHomeInfo = true },
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.AddToHomeScreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.add_to_home),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        // Piece controls live in the bottom bar, not floating over the board:
        // as an overlay they sat underneath the FAB and were unreachable (#3, #7).
        // Scaffold keeps the FAB clear of a bottom bar for us.
        bottomBar = {
            AnimatedVisibility(
                visible = state.selectedId != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                val id = state.selectedId
                val selectedText = state.selectedText
                if (selectedText != null) {
                    TextControls(
                        piece = selectedText,
                        onEdit = { editingTextId = selectedText.id },
                        onFront = { id?.let(viewModel::bringToFront) },
                        onBack = { id?.let(viewModel::sendToBack) },
                        onDuplicate = { id?.let(viewModel::duplicate) },
                        onDelete = { id?.let(viewModel::delete) },
                        onUpdate = { transform -> id?.let { viewModel.updateText(it, transform) } },
                    )
                } else {
                    PieceControls(
                        onFront = { id?.let(viewModel::bringToFront) },
                        onBack = { id?.let(viewModel::sendToBack) },
                        onDuplicate = { id?.let(viewModel::duplicate) },
                        onDelete = { id?.let(viewModel::delete) },
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                icon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null) },
                text = { Text(stringResource(R.string.add_photos)) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CollageBoard(
                    state = state,
                    onSelect = viewModel::select,
                    onTransform = viewModel::transform,
                    onTransformText = viewModel::transformText,
                    onClockDrag = viewModel::nudgeClock,
                )
            }

            if (state.isProcessing) {
                BatchProgress(
                    done = state.processedCount,
                    total = state.batchSize,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/** Batch progress (#9): "Cutting out 2 of 5" rather than an opaque spinner. */
@Composable
private fun BatchProgress(done: Int, total: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = if (total > 1) {
                    stringResource(R.string.processing_batch, done + 1, total)
                } else {
                    stringResource(R.string.processing)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CollageBoard(
    state: EditorUiState,
    onSelect: (String?) -> Unit,
    onTransform: (id: String, dxFrac: Float, dyFrac: Float, zoom: Float, rot: Float) -> Unit,
    onTransformText: (id: String, dxFrac: Float, dyFrac: Float, zoom: Float, rot: Float) -> Unit,
    onClockDrag: (dxFrac: Float, dyFrac: Float) -> Unit,
) {
    val aspect = state.collage.aspect.ratio
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(MaterialTheme.shapes.medium)
            // A checkerboard behind a transparent board, so "no background"
            // reads as see-through rather than as a grey fill.
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Tap empty board to deselect.
            .pointerInputTap { onSelect(null) },
    ) {
        val boardW = constraints.maxWidth.toFloat()
        val boardH = constraints.maxHeight.toFloat()

        BackgroundPreview(
            background = state.collage.background,
            cornerRadius = state.collage.cornerRadius,
            modifier = Modifier.matchParentSize(),
        )

        // Photos and text share one z-order, matching the render exactly.
        val layers = remember(state.collage.pieces, state.collage.texts) {
            (state.collage.pieces + state.collage.texts).sortedBy {
                when (it) {
                    is CollagePiece -> it.zIndex
                    is TextPiece -> it.zIndex
                    else -> 0
                }
            }
        }

        layers.forEach { layer ->
            when (layer) {
                is CollagePiece -> PieceView(
                    piece = layer,
                    isSelected = layer.id == state.selectedId,
                    boardW = boardW,
                    boardH = boardH,
                    onSelect = { onSelect(layer.id) },
                    onTransform = onTransform,
                )
                is TextPiece -> TextPieceView(
                    piece = layer,
                    isSelected = layer.id == state.selectedId,
                    boardW = boardW,
                    boardH = boardH,
                    onSelect = { onSelect(layer.id) },
                    onTransform = onTransformText,
                )
            }
        }

        // Clock sits above the pieces, as it does on the widget.
        ClockPreview(
            clock = state.collage.clock,
            modifier = Modifier.matchParentSize(),
        )
        ClockDragHandle(
            state = state,
            boardW = boardW,
            boardH = boardH,
            onDrag = onClockDrag,
        )
    }
}

/**
 * An invisible (lightly outlined) box over the clock text that makes it
 * draggable anywhere on the board. Sized from the same measurement the static
 * renderer uses, so the hit-target hugs the text.
 */
@Composable
private fun ClockDragHandle(
    state: EditorUiState,
    boardW: Float,
    boardH: Float,
    onDrag: (dxFrac: Float, dyFrac: Float) -> Unit,
) {
    val clock = state.collage.clock
    if (!clock.enabled) return
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val density = LocalDensity.current

    val (blockW, blockH) = remember(clock, boardW, boardH) {
        ClockRenderer.measure(boardW.toInt().coerceAtLeast(1), boardH.toInt().coerceAtLeast(1), clock, is24Hour)
    }
    val padPx = with(density) { 8.dp.toPx() }
    val w = blockW + padPx * 2
    val h = blockH + padPx * 2

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (clock.posX * boardW - w / 2f).roundToInt(),
                    (clock.posY * boardH - h / 2f).roundToInt(),
                )
            }
            .requiredSize(with(density) { w.toDp() }, with(density) { h.toDp() })
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                MaterialTheme.shapes.small,
            )
            .pointerInput(boardW, boardH) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x / boardW, drag.y / boardH)
                }
            },
    )
}

@Composable
private fun PieceView(
    piece: CollagePiece,
    isSelected: Boolean,
    boardW: Float,
    boardH: Float,
    onSelect: () -> Unit,
    onTransform: (id: String, dxFrac: Float, dyFrac: Float, zoom: Float, rot: Float) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val painter = rememberAsyncImagePainter(File(piece.cutoutPath))
    // pointerInput captures its lambda when its keys change, so reading
    // piece.rotation directly inside the gesture would freeze at the angle the
    // piece had when it was selected. This stays live.
    val rotation by rememberUpdatedState(piece.rotation)
    val currentSize by rememberUpdatedState(size)
    val density = LocalDensity.current

    // requiredSize, not fillMaxWidth(fraction) — and *both* dimensions matter.
    // fillMaxWidth coerced the piece into the parent's constraints, so growing
    // past the board did nothing. Width alone wasn't enough either: with only
    // requiredWidth, the Image's height still capped at the board's height, so
    // a big piece grew its selection box left-right while the drawn image froze
    // — exactly the "handles grow but the image doesn't" bug. Deriving height
    // from the cutout's own aspect ratio frees both axes, so a piece can cover
    // the whole board and overhang it.
    val intrinsic = painter.intrinsicSize
    val imageAspect = if (intrinsic.isSpecified() && intrinsic.width > 0f) {
        intrinsic.height / intrinsic.width
    } else {
        1f
    }
    val widthPx = piece.scale * boardW
    val heightPx = widthPx * imageAspect
    val pieceWidth = with(density) { widthPx.toDp() }
    val pieceHeight = with(density) { heightPx.toDp() }

    Box(
        modifier = Modifier
            .requiredSize(pieceWidth, pieceHeight)
            .onSizeChanged { size = it }
            .offset {
                IntOffset(
                    (piece.centerX * boardW - size.width / 2f).roundToInt(),
                    (piece.centerY * boardH - size.height / 2f).roundToInt(),
                )
            }
            .graphicsLayer { rotationZ = piece.rotation }
            .pointerInputTap { onSelect() }
            .pointerInputTransform(piece.id, isSelected) { pan, zoom, rot ->
                onSelect()
                // The gesture is reported inside the piece's rotated frame, so a
                // rotated piece would otherwise drift off at an angle from your
                // finger. Rotate the pan back into board space before applying.
                val rad = Math.toRadians(rotation.toDouble())
                val cos = cos(rad).toFloat()
                val sin = sin(rad).toFloat()
                val dx = pan.x * cos - pan.y * sin
                val dy = pan.x * sin + pan.y * cos
                onTransform(piece.id, dx / boardW, dy / boardH, zoom, rot)
            },
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        if (isSelected) {
            SelectionChrome(
                onScale = { factor -> onTransform(piece.id, 0f, 0f, factor, 0f) },
                sizeProvider = { currentSize },
            )
        }
    }
}

private fun androidx.compose.ui.geometry.Size.isSpecified(): Boolean =
    this != androidx.compose.ui.geometry.Size.Unspecified && width > 0f && height > 0f

@Composable
private fun TextPieceView(
    piece: TextPiece,
    isSelected: Boolean,
    boardW: Float,
    boardH: Float,
    onSelect: () -> Unit,
    onTransform: (id: String, dxFrac: Float, dyFrac: Float, zoom: Float, rot: Float) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val rotation by rememberUpdatedState(piece.rotation)
    val currentSize by rememberUpdatedState(size)
    val density = LocalDensity.current

    val shortEdge = minOf(boardW, boardH)
    val fontPx = shortEdge * piece.sizeFraction
    val fontFamily = remember(piece.font) { FontFamily(piece.font.typeface()) }

    // Sized from the same measurement the renderer uses, so the box (and its
    // selection border) hugs the text exactly as it will export.
    val (blockW, blockH) = remember(piece.text, piece.font, piece.sizeFraction, boardW, boardH) {
        TextRenderer.measure(boardW.toInt().coerceAtLeast(1), boardH.toInt().coerceAtLeast(1), piece)
    }

    Box(
        modifier = Modifier
            .requiredSize(with(density) { blockW.toDp() }, with(density) { blockH.toDp() })
            .onSizeChanged { size = it }
            .offset {
                IntOffset(
                    (piece.centerX * boardW - size.width / 2f).roundToInt(),
                    (piece.centerY * boardH - size.height / 2f).roundToInt(),
                )
            }
            .graphicsLayer { rotationZ = piece.rotation }
            .pointerInputTap { onSelect() }
            .pointerInputTransform(piece.id, isSelected) { pan, zoom, rot ->
                onSelect()
                val rad = Math.toRadians(rotation.toDouble())
                val cos = cos(rad).toFloat()
                val sin = sin(rad).toFloat()
                val dx = pan.x * cos - pan.y * sin
                val dy = pan.x * sin + pan.y * cos
                onTransform(piece.id, dx / boardW, dy / boardH, zoom, rot)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = piece.text,
            color = Color(piece.color),
            fontFamily = fontFamily,
            fontSize = with(density) { fontPx.toSp() },
            lineHeight = with(density) { (fontPx * 1.1f).toSp() },
            textAlign = TextAlign.Center,
            softWrap = false,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(
                    color = Color(0x80000000),
                    offset = Offset(0f, fontPx * 0.04f),
                    blurRadius = fontPx * 0.12f,
                ),
            ),
        )

        if (isSelected) {
            SelectionChrome(
                onScale = { factor -> onTransform(piece.id, 0f, 0f, factor, 0f) },
                sizeProvider = { currentSize },
            )
        }
    }
}

/** Selection border + the four corner resize handles. */
@Composable
private fun BoxScope.SelectionChrome(
    onScale: (Float) -> Unit,
    sizeProvider: () -> IntSize,
) {
    Box(
        Modifier
            .matchParentSize()
            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
    )
    Corner.entries.forEach { corner ->
        ResizeHandle(
            corner = corner,
            onScale = onScale,
            sizeProvider = sizeProvider,
        )
    }
}

/** Which corner a [ResizeHandle] lives on, and its direction from the centre. */
private enum class Corner(val alignment: Alignment, val signX: Float, val signY: Float) {
    TOP_START(Alignment.TopStart, -1f, -1f),
    TOP_END(Alignment.TopEnd, 1f, -1f),
    BOTTOM_START(Alignment.BottomStart, -1f, 1f),
    BOTTOM_END(Alignment.BottomEnd, 1f, 1f),
}

/**
 * Drag a corner to resize the piece itself (#8).
 *
 * Scaling is uniform about the centre: project the drag onto the corner's
 * outward diagonal and grow the half-diagonal by that much. Because the drag and
 * the corner vector are both in the piece's own rotated frame, this needs no
 * rotation maths — and reading the live size each event means it tracks the
 * finger instead of drifting from a stale start value.
 */
@Composable
private fun BoxScope.ResizeHandle(
    corner: Corner,
    onScale: (Float) -> Unit,
    sizeProvider: () -> IntSize,
) {
    val handleSize = 22.dp
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .align(corner.alignment)
            .offset(x = handleSize / 2 * corner.signX, y = handleSize / 2 * corner.signY)
            .size(handleSize)
            .pointerInput(corner) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val s = sizeProvider()
                    val vx = s.width / 2f * corner.signX
                    val vy = s.height / 2f * corner.signY
                    val len = hypot(vx, vy)
                    if (len < 1f) return@detectDragGestures
                    // Component of the drag along the outward diagonal.
                    val along = (drag.x * vx + drag.y * vy) / len
                    onScale(((len + along) / len).coerceIn(0.5f, 1.5f))
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
        )
    }
}

@Composable
private fun PieceControls(
    onFront: () -> Unit,
    onBack: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.padding(16.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilledTonalIconButton(onClick = onFront) {
                Icon(Icons.Filled.FlipToFront, stringResource(R.string.bring_to_front))
            }
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.Filled.FlipToBack, stringResource(R.string.send_to_back))
            }
            FilledTonalIconButton(onClick = onDuplicate) {
                Icon(Icons.Filled.ContentCopy, stringResource(R.string.duplicate_piece))
            }
            FilledTonalIconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete_piece))
            }
        }
    }
}

/** Controls for a selected text piece: actions plus font and colour. */
@Composable
private fun TextControls(
    piece: TextPiece,
    onEdit: () -> Unit,
    onFront: () -> Unit,
    onBack: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: ((TextPiece) -> TextPiece) -> Unit,
) {
    Surface(
        modifier = Modifier.padding(16.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, stringResource(R.string.edit_text))
                }
                FilledTonalIconButton(onClick = onFront) {
                    Icon(Icons.Filled.FlipToFront, stringResource(R.string.bring_to_front))
                }
                FilledTonalIconButton(onClick = onBack) {
                    Icon(Icons.Filled.FlipToBack, stringResource(R.string.send_to_back))
                }
                FilledTonalIconButton(onClick = onDuplicate) {
                    Icon(Icons.Filled.ContentCopy, stringResource(R.string.duplicate_piece))
                }
                FilledTonalIconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete_piece))
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverlayFont.entries.forEach { font ->
                    FilterChip(
                        selected = font == piece.font,
                        onClick = { onUpdate { it.copy(font = font) } },
                        label = { Text(font.label, fontFamily = FontFamily(font.typeface())) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OVERLAY_COLORS.forEach { colorValue ->
                    val selected = colorValue == piece.color
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                            .clickable { onUpdate { it.copy(color = colorValue) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun TextEntryDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial.isEmpty()) R.string.add_text else R.string.edit_text))
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(stringResource(R.string.text_hint)) },
                minLines = 1,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value) },
            ) { Text(stringResource(R.string.done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * The Layers sheet (#tested3): every photo and text on the board, front-most
 * first. Solves "I can't tap the piece I want" — overlapping pieces and
 * handles make canvas selection ambiguous, but a list never is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayersSheet(
    collage: Collage,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, up: Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Front-most layer first — the order you'd peel them off the board.
    val layers = (collage.pieces.map { LayerRow(it.id, it.zIndex, it) } +
        collage.texts.map { LayerRow(it.id, it.zIndex, it) })
        .sortedByDescending { it.z }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.layers), style = MaterialTheme.typography.titleMedium)
            if (layers.isEmpty()) {
                Text(
                    stringResource(R.string.layers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(layers, key = { it.id }) { row ->
                    LayerRowItem(
                        row = row,
                        selected = row.id == selectedId,
                        onSelect = { onSelect(row.id) },
                        onMove = { up -> onMove(row.id, up) },
                        onDelete = { onDelete(row.id) },
                    )
                }
            }
        }
    }
}

private data class LayerRow(val id: String, val z: Int, val payload: Any)

@Composable
private fun LayerRowItem(
    row: LayerRow,
    selected: Boolean,
    onSelect: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val payload = row.payload) {
                is CollagePiece -> {
                    Image(
                        painter = rememberAsyncImagePainter(File(payload.cutoutPath)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surface),
                    )
                    Text(
                        text = stringResource(R.string.layer_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                is TextPiece -> {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Aa",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily(payload.font.typeface()),
                        )
                    }
                    Text(
                        text = payload.text.replace("\n", " "),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            IconButton(onClick = { onMove(true) }) {
                Icon(Icons.Filled.ArrowUpward, stringResource(R.string.move_layer_up))
            }
            IconButton(onClick = { onMove(false) }) {
                Icon(Icons.Filled.ArrowDownward, stringResource(R.string.move_layer_down))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(R.string.delete_piece))
            }
        }
    }
}
