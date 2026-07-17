package com.papercut.collage.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.papercut.collage.R
import com.papercut.collage.model.Collage
import com.papercut.collage.model.CollageTemplate
import com.papercut.collage.ui.common.CollageThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewCollage: () -> Unit,
    onOpenCollage: (String) -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val collages by viewModel.collages.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Collage?>(null) }
    var showTemplates by remember { mutableStateOf(false) }

    if (showTemplates) {
        TemplateSheet(
            onPick = { template ->
                showTemplates = false
                if (template == CollageTemplate.BLANK) {
                    // Nothing worth keeping yet — don't put an empty card on Home
                    // until it has a piece.
                    onNewCollage()
                } else {
                    viewModel.createFromTemplate(template.toCollage()) { id -> onOpenCollage(id) }
                }
            },
            onDismiss = { showTemplates = false },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_collage_title, target.name)) },
            text = { Text(stringResource(R.string.delete_collage_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete_piece)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTemplates = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_collage)) },
            )
        },
    ) { padding ->
        if (collages.isEmpty()) {
            EmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(collages, key = { _, c -> c.id }) { index, collage ->
                    CollageCard(
                        collage = collage,
                        index = index,
                        onClick = { onOpenCollage(collage.id) },
                        onLongClick = { pendingDelete = collage },
                    )
                }
            }
        }
    }
}

/**
 * Tap to open, long-press to delete (#6).
 *
 * Cards get a tiny alternating tilt and cycling container tints, so the grid
 * reads like paper scraps laid on a table rather than a spreadsheet — the same
 * energy as the collages themselves.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollageCard(collage: Collage, index: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    val tilt = CARD_TILTS[index % CARD_TILTS.size]
    val container = when (index % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.graphicsLayer { rotationZ = tilt },
    ) {
        Column(
            modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            CollageThumbnail(
                collage = collage,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(collage.aspect.ratio)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Text(
                text = collage.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** Small, varied tilts — enough to feel hand-placed, not enough to look broken. */
private val CARD_TILTS = listOf(-1.4f, 1.1f, 0.8f, -0.9f, 1.6f, -0.6f)

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp).graphicsLayer { rotationZ = -6f },
            )
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
