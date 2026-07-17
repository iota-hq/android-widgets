package com.papercut.collage.ui.editor

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.papercut.collage.data.CollageRepository
import com.papercut.collage.model.BoardAspect
import com.papercut.collage.model.BoardBackground
import com.papercut.collage.model.ClockOverlay
import com.papercut.collage.model.Collage
import com.papercut.collage.model.CollagePiece
import com.papercut.collage.model.TextPiece
import com.papercut.collage.render.AlphaCrop
import com.papercut.collage.render.PaperEdgeProcessor
import com.papercut.collage.segmentation.SubjectSegmenter
import com.papercut.collage.widget.CollageWidgetProvider
import com.papercut.collage.widget.CollageWidgetUpdater
import com.papercut.collage.widget.PendingPin
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val collage: Collage = Collage(),
    val selectedId: String? = null,
    val loaded: Boolean = false,
    /** True once this collage exists in the DB — see the autosave gate. */
    val persisted: Boolean = false,
    val error: String? = null,
    /** Photos finished in the current batch, and how many were picked (#9). */
    val processedCount: Int = 0,
    val batchSize: Int = 0,
) {
    val isProcessing: Boolean get() = batchSize > 0

    /** The selected photo piece, if the selection is one. */
    val selectedPiece: CollagePiece? get() = collage.pieces.find { it.id == selectedId }

    /** The selected text piece, if the selection is one. */
    val selectedText: TextPiece? get() = collage.texts.find { it.id == selectedId }
}

/**
 * Drives the editor: load/create a collage, pick photo → segment →
 * paper-cutout → add piece, and transform/reorder pieces. Changes autosave
 * (debounced) to Room so the collage survives and can feed the widget.
 */
@OptIn(FlowPreview::class)
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val segmenter = SubjectSegmenter()
    private val repo = CollageRepository.from(app)

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        // Debounced autosave: persist the collage shortly after it settles.
        viewModelScope.launch {
            _state
                .map { it.collage }
                .distinctUntilChanged()
                .drop(1) // ignore the initial/loaded value
                .debounce(500)
                .collect { collage ->
                    // Save once there's something worth keeping: a piece or a
                    // text, or a collage that already exists (opened from Home,
                    // or created from a template — those carry choices before
                    // any photo).
                    if (collage.pieces.isEmpty() && collage.texts.isEmpty() &&
                        !_state.value.persisted
                    ) {
                        return@collect
                    }
                    repo.save(collage)
                    _state.update { it.copy(persisted = true) }
                    // Any widget showing this collage re-renders with the edit.
                    CollageWidgetUpdater.updateForCollage(getApplication(), collage.id)
                }
        }
    }

    /** Called once from the screen with the nav argument. */
    fun load(collageId: String) {
        if (_state.value.loaded) return
        viewModelScope.launch {
            val existing = if (collageId != NEW) repo.load(collageId) else null
            _state.update {
                it.copy(
                    collage = existing ?: Collage(),
                    loaded = true,
                    persisted = existing != null,
                )
            }
        }
    }

    fun select(id: String?) = _state.update { it.copy(selectedId = id) }

    /**
     * Ask the launcher to pin a widget for this collage. The collage is saved
     * first (the widget renders from Room) and its id is parked for
     * [com.papercut.collage.widget.WidgetConfigActivity] to claim, since the
     * launcher assigns the appWidgetId only after the user confirms.
     */
    fun addToHomeScreen(onUnsupported: () -> Unit = {}) {
        val app = getApplication<Application>()
        val manager = AppWidgetManager.getInstance(app)
        if (!manager.isRequestPinAppWidgetSupported) {
            onUnsupported()
            return
        }
        viewModelScope.launch {
            val collage = _state.value.collage
            repo.save(collage)
            PendingPin.set(app, collage.id)
            manager.requestPinAppWidget(
                ComponentName(app, CollageWidgetProvider::class.java),
                null,
                null,
            )
        }
    }

    /**
     * Segments each picked photo **one at a time**, adding each piece as it
     * finishes. Sequential on purpose: segmentation is memory-hungry, and a
     * parallel batch of full-size bitmaps is a good way to get OOM-killed. Each
     * piece lands in the collage as soon as it's ready, so the board fills in
     * progressively instead of after a long silence.
     */
    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(batchSize = uris.size, processedCount = 0, error = null) }
            var failures = 0
            uris.forEach { uri ->
                try {
                    val piece = withContext(Dispatchers.Default) { processToPiece(uri) }
                    _state.update { s ->
                        s.copy(
                            collage = s.collage.copy(
                                pieces = s.collage.pieces + piece.copy(zIndex = topZ(s.collage) + 1),
                            ),
                            selectedId = piece.id,
                            processedCount = s.processedCount + 1,
                        )
                    }
                } catch (e: Exception) {
                    // One bad photo shouldn't abandon the rest of the batch.
                    failures++
                    _state.update { it.copy(processedCount = it.processedCount + 1) }
                }
            }
            _state.update { s ->
                s.copy(
                    batchSize = 0,
                    processedCount = 0,
                    error = if (failures > 0) "Couldn't cut out $failures of ${uris.size} photos" else null,
                )
            }
        }
    }

    fun setAspect(aspect: BoardAspect) =
        _state.update { it.copy(collage = it.collage.copy(aspect = aspect)) }

    fun setBackground(background: BoardBackground) =
        _state.update { it.copy(collage = it.collage.copy(background = background)) }

    fun setCornerRadius(radius: Float) =
        _state.update { it.copy(collage = it.collage.copy(cornerRadius = radius.coerceIn(0f, 1f))) }

    fun setClock(clock: ClockOverlay) =
        _state.update { it.copy(collage = it.collage.copy(clock = clock)) }

    /** Drag delta from the editor board, in board fractions. */
    fun nudgeClock(dxFrac: Float, dyFrac: Float) {
        val clock = _state.value.collage.clock
        if (!clock.enabled) return
        setClock(
            clock.copy(
                posX = (clock.posX + dxFrac).coerceIn(0.02f, 0.98f),
                posY = (clock.posY + dyFrac).coerceIn(0.02f, 0.98f),
            ),
        )
    }

    /** Copies a picked background photo into app storage so it survives. */
    fun setBackgroundFromGallery(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val ctx = getApplication<Application>()
                    val source = ctx.contentResolver.openInputStream(uri).use {
                        BitmapFactory.decodeStream(it)
                    } ?: error("Could not read image")
                    val scaled = downscale(source, maxEdge = 1536)
                    val file = File(ctx.filesDir, "bg_${System.nanoTime()}.png")
                    FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    file.absolutePath
                }
            }.onSuccess { path ->
                setBackground(BoardBackground.Image(path))
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /**
     * Combined pan/zoom/rotate from a transform gesture. Pan arrives already
     * converted to board space by the caller (the gesture is reported in the
     * piece's rotated frame).
     */
    fun transform(id: String, panXFrac: Float, panYFrac: Float, zoom: Float, rotationDeg: Float) {
        updatePiece(id) {
            it.copy(
                centerX = (it.centerX + panXFrac).coerceIn(0f, 1f),
                centerY = (it.centerY + panYFrac).coerceIn(0f, 1f),
                // Up to 6× the board width — effectively unlimited. A piece may
                // cover the whole board and overhang it on every side.
                scale = (it.scale * zoom).coerceIn(0.05f, 6f),
                rotation = it.rotation + rotationDeg,
            )
        }
    }

    // --- Text pieces -------------------------------------------------------

    fun addText(text: String) {
        if (text.isBlank()) return
        val piece = TextPiece(text = text.trim(), zIndex = topZ(_state.value.collage) + 1)
        _state.update { s ->
            s.copy(
                collage = s.collage.copy(texts = s.collage.texts + piece),
                selectedId = piece.id,
            )
        }
    }

    fun updateText(id: String, transform: (TextPiece) -> TextPiece) {
        _state.update { s ->
            s.copy(collage = s.collage.copy(texts = s.collage.texts.map {
                if (it.id == id) transform(it) else it
            }))
        }
    }

    /** Pan/zoom/rotate for a text piece; zoom scales the font size. */
    fun transformText(id: String, panXFrac: Float, panYFrac: Float, zoom: Float, rotationDeg: Float) {
        updateText(id) {
            it.copy(
                centerX = (it.centerX + panXFrac).coerceIn(0f, 1f),
                centerY = (it.centerY + panYFrac).coerceIn(0f, 1f),
                sizeFraction = (it.sizeFraction * zoom).coerceIn(0.03f, 0.6f),
                rotation = it.rotation + rotationDeg,
            )
        }
    }

    // --- Layering (shared z-order between photos and text) ------------------

    fun bringToFront(id: String) = reZ(id) { topZ(_state.value.collage) + 1 }

    fun sendToBack(id: String) = reZ(id) { bottomZ(_state.value.collage) - 1 }

    /** Swap z with the layer directly above/below — used by the Layers sheet. */
    fun moveLayer(id: String, up: Boolean) {
        val c = _state.value.collage
        val ordered = layersOf(c).sortedBy { it.second }
        val index = ordered.indexOfFirst { it.first == id }
        if (index < 0) return
        val neighbour = if (up) index + 1 else index - 1
        if (neighbour !in ordered.indices) return
        val (idA, zA) = ordered[index]
        val (idB, zB) = ordered[neighbour]
        // Swap the two z values; if they collide (legacy data), force distinct.
        val newZA = if (zA == zB) (if (up) zB + 1 else zB - 1) else zB
        reZ(idA) { newZA }
        reZ(idB) { zA }
    }

    /** All layer ids with their z, photos and text together. */
    private fun layersOf(c: Collage): List<Pair<String, Int>> =
        c.pieces.map { it.id to it.zIndex } + c.texts.map { it.id to it.zIndex }

    private fun reZ(id: String, newZ: () -> Int) {
        val c = _state.value.collage
        when {
            c.pieces.any { it.id == id } -> updatePiece(id) { it.copy(zIndex = newZ()) }
            c.texts.any { it.id == id } -> updateText(id) { it.copy(zIndex = newZ()) }
        }
    }

    fun duplicate(id: String) {
        val s0 = _state.value.collage
        s0.pieces.find { it.id == id }?.let { src ->
            val copy = src.copy(
                id = java.util.UUID.randomUUID().toString(),
                centerX = (src.centerX + 0.06f).coerceIn(0f, 1f),
                centerY = (src.centerY + 0.06f).coerceIn(0f, 1f),
                zIndex = topZ(s0) + 1,
            )
            _state.update { s ->
                s.copy(collage = s.collage.copy(pieces = s.collage.pieces + copy), selectedId = copy.id)
            }
            return
        }
        s0.texts.find { it.id == id }?.let { src ->
            val copy = src.copy(
                id = java.util.UUID.randomUUID().toString(),
                centerX = (src.centerX + 0.06f).coerceIn(0f, 1f),
                centerY = (src.centerY + 0.06f).coerceIn(0f, 1f),
                zIndex = topZ(s0) + 1,
            )
            _state.update { s ->
                s.copy(collage = s.collage.copy(texts = s.collage.texts + copy), selectedId = copy.id)
            }
        }
    }

    fun delete(id: String) {
        _state.update { s ->
            s.copy(
                collage = s.collage.copy(
                    pieces = s.collage.pieces.filterNot { it.id == id },
                    texts = s.collage.texts.filterNot { it.id == id },
                ),
                selectedId = null,
            )
        }
    }

    private fun updatePiece(id: String, transform: (CollagePiece) -> CollagePiece) {
        _state.update { s ->
            s.copy(collage = s.collage.copy(pieces = s.collage.pieces.map {
                if (it.id == id) transform(it) else it
            }))
        }
    }

    private fun topZ(c: Collage) =
        (c.pieces.map { it.zIndex } + c.texts.map { it.zIndex }).maxOrNull() ?: 0

    private fun bottomZ(c: Collage) =
        (c.pieces.map { it.zIndex } + c.texts.map { it.zIndex }).minOrNull() ?: 0

    private suspend fun processToPiece(uri: Uri): CollagePiece {
        val ctx = getApplication<Application>()
        val source = ctx.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it)
        } ?: error("Could not read image")

        val working = downscale(source, maxEdge = 1536)
        val cutout = segmenter.cutout(working) ?: error("No subject found")
        // ML Kit returns the whole frame with the background cleared, so trim to
        // the subject before styling it — otherwise the piece's bounds are the
        // original photo and everything downstream (border, scale, file size)
        // measures empty space.
        val trimmed = AlphaCrop.cropToContent(cutout)
        val seed = System.nanoTime()
        val paper = PaperEdgeProcessor.toPaperCutout(trimmed, seed)

        val file = File(ctx.filesDir, "cutout_${seed}.png")
        FileOutputStream(file).use { paper.compress(Bitmap.CompressFormat.PNG, 100, it) }

        return CollagePiece(cutoutPath = file.absolutePath, edgeSeed = seed)
    }

    private fun downscale(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = maxOf(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val scale = maxEdge.toFloat() / longEdge
        return Bitmap.createScaledBitmap(
            src, (src.width * scale).toInt(), (src.height * scale).toInt(), true,
        )
    }

    override fun onCleared() {
        segmenter.close()
    }

    companion object {
        const val NEW = "new"
    }
}
