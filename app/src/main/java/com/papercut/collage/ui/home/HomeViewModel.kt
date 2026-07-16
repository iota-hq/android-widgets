package com.papercut.collage.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.papercut.collage.data.CollageRepository
import com.papercut.collage.data.WidgetRepository
import com.papercut.collage.model.Collage
import com.papercut.collage.widget.CollageWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CollageRepository.from(app)
    private val widgetRepo = WidgetRepository.from(app)

    val collages: StateFlow<List<Collage>> =
        repo.observeCollages()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Persists a template-derived collage and hands back its id to open.
     *
     * Saved up front (unlike a blank collage, which only persists once it has a
     * piece) because a template already carries choices worth keeping, and the
     * editor loads by id.
     */
    fun createFromTemplate(collage: Collage, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repo.save(collage)
            onCreated(collage.id)
        }
    }

    /**
     * Deletes a collage and releases any widgets showing it, so none are left
     * pointing at an id that no longer exists. The widgets stay on the home
     * screen (only their host can remove them) but re-render empty and can be
     * reconfigured onto another collage.
     */
    fun delete(collageId: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val orphaned = withContext(Dispatchers.Default) {
                val ids = widgetRepo.widgetIdsFor(collageId)
                repo.delete(collageId)
                ids.forEach { widgetRepo.unbind(it) }
                ids
            }
            CollageWidgetProvider.requestUpdate(app, orphaned.toIntArray())
        }
    }
}
