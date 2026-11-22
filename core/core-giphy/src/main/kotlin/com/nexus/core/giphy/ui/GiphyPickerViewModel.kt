package com.nexus.core.giphy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.giphy.GiphyItem
import com.nexus.core.giphy.GiphyKind
import com.nexus.core.giphy.GiphyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class GiphyPickerState(
    val kind: GiphyKind = GiphyKind.GIF,
    val query: String = "",
    val items: List<GiphyItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val isAvailable: Boolean = true,
)

@HiltViewModel
class GiphyPickerViewModel @Inject constructor(
    private val repository: GiphyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GiphyPickerState(isAvailable = repository.isAvailable))
    val state: StateFlow<GiphyPickerState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        load(debounce = false)
    }

    fun onKindChanged(kind: GiphyKind) {
        if (kind == _state.value.kind) return
        _state.update { it.copy(kind = kind, items = emptyList()) }
        load(debounce = false)
    }

    fun onQueryChanged(query: String) {
        if (query == _state.value.query) return
        _state.update { it.copy(query = query) }
        load(debounce = true)
    }

    fun retry() = load(debounce = false)

    // Only the latest search counts: an older, slower response must not overwrite it
    private fun load(debounce: Boolean) {
        if (!repository.isAvailable) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(DEBOUNCE_MS)
            val (kind, query) = _state.value.let { it.kind to it.query }
            _state.update { it.copy(isLoading = true, hasError = false) }
            try {
                val items = repository.search(kind, query)
                _state.update { it.copy(items = items, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Giphy search failed")
                _state.update { it.copy(items = emptyList(), isLoading = false, hasError = true) }
            }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 350L
    }
}
