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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
                title = { Text(stringResource(R.string.home_title)) },
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(collages, key = { it.id }) { collage ->
                    CollageCard(
                        collage = collage,
                        onClick = { onOpenCollage(collage.id) },
                        onLongClick = { pendingDelete = collage },
                    )
                }
            }
        }
    }
}

/** Tap to open, long-press to delete (#6). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollageCard(collage: Collage, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card {
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
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}
