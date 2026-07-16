package com.papercut.collage.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.papercut.collage.R
import com.papercut.collage.model.CollagePiece
import java.io.File
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/** Photo Picker cap for one batch — enough for a dense collage, bounded work. */
private const val MAX_PHOTOS_PER_PICK = 10

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
    var showAddToHomeInfo by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
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
                        enabled = state.collage.pieces.isNotEmpty(),
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
                PieceControls(
                    onFront = { id?.let(viewModel::bringToFront) },
                    onBack = { id?.let(viewModel::sendToBack) },
                    onDuplicate = { id?.let(viewModel::duplicate) },
                    onDelete = { id?.let(viewModel::delete) },
                )
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

        state.collage.pieces.sortedBy { it.zIndex }.forEach { piece ->
            PieceView(
                piece = piece,
                isSelected = piece.id == state.selectedId,
                boardW = boardW,
                boardH = boardH,
                onSelect = { onSelect(piece.id) },
                onTransform = onTransform,
            )
        }

        // Clock sits above the pieces, as it does on the widget.
        ClockPreview(
            clock = state.collage.clock,
            modifier = Modifier.matchParentSize(),
        )
    }
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

    // requiredWidth, not fillMaxWidth(fraction): fillMaxWidth coerces the result
    // into the parent's constraints, so a piece could never exceed the board's
    // width — growing past scale 1.0 did nothing at all, however far you dragged.
    // requiredWidth ignores the parent's max, which also lets a cutout overhang
    // the board edge the way a real pasted-on piece would.
    val pieceWidth = with(LocalDensity.current) { (piece.scale * boardW).toDp() }

    Box(
        modifier = Modifier
            .requiredWidth(pieceWidth)
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
            modifier = Modifier.fillMaxWidth(),
        )

        if (isSelected) {
            // The box now hugs the cutout (see AlphaCrop), so this outline sits
            // on the paper's edge rather than around the old photo frame.
            Box(
                Modifier
                    .matchParentSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
            )
            Corner.entries.forEach { corner ->
                ResizeHandle(
                    corner = corner,
                    onScale = { factor -> onTransform(piece.id, 0f, 0f, factor, 0f) },
                    sizeProvider = { currentSize },
                )
            }
        }
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
