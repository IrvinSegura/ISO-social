package com.nexus.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.core.common.event.NewPostEventBus
import com.nexus.core.common.event.PostChange
import com.nexus.core.datastore.AppPreferencesDataStore
import com.nexus.feature.profile.domain.model.ProfilePost
import com.nexus.feature.profile.domain.model.ProfileStats
import com.nexus.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val preferences: AppPreferencesDataStore,
    private val eventBus: NewPostEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Saved posts already fetched, so a new save or unsave only reads what changed
    private val savedCache = mutableMapOf<String, ProfilePost>()
    private var savedIds: List<String> = emptyList()
    private var savedIdsLoaded = false
    private var savedJob: Job? = null
    private var postsJob: Job? = null

    init {
        loadUser()
        loadMyPosts()
        observeSaved()
        observeTheme()
        observePostEvents()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.SelectTab -> _state.update { it.copy(selectedTab = intent.tab) }
            ProfileIntent.Refresh -> refresh()
            ProfileIntent.RetryPosts -> loadMyPosts()
            ProfileIntent.RetrySaved -> {
                savedCache.clear()
                resolveSaved(savedIds)
            }
            is ProfileIntent.Unsave -> setSaved(intent.postId, saved = false)
            is ProfileIntent.UndoUnsave -> setSaved(intent.postId, saved = true)
            is ProfileIntent.SetTheme -> viewModelScope.launch { preferences.setThemeMode(intent.mode) }
        }
    }

    private fun refresh() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            coroutineScope {
                launch { loadUserNow() }
                launch { loadMyPostsNow() }
                launch {
                    savedCache.clear()
                    resolveSavedNow(savedIds)
                }
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadUser() {
        viewModelScope.launch { loadUserNow() }
    }

    private suspend fun loadUserNow() {
        try {
            val user = repository.currentUser()
            _state.update { it.copy(user = user) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not load the profile")
        }
    }

    private fun loadMyPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch { loadMyPostsNow() }
    }

    private suspend fun loadMyPostsNow() {
        _state.update { it.copy(myPosts = it.myPosts.copy(isLoading = it.myPosts.items.isEmpty(), error = null)) }
        try {
            val result = repository.myPosts()
            _state.update {
                it.copy(
                    myPosts = TabContent(items = result.posts, isLoading = false),
                    stats = ProfileStats.from(result.posts, isPartial = result.hasMore),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not load own posts")
            _state.update {
                it.copy(myPosts = it.myPosts.copy(isLoading = false, error = "No se pudieron cargar tus publicaciones"))
            }
        }
    }

    private fun observeSaved() {
        viewModelScope.launch {
            repository.observeSavedIds()
                .distinctUntilChanged()
                .catch { e ->
                    Timber.w(e, "Saved posts listener failed")
                    _state.update {
                        it.copy(saved = it.saved.copy(isLoading = false, error = "No se pudieron cargar tus guardados"))
                    }
                }
                .collect { ids ->
                    savedIds = ids
                    savedIdsLoaded = true
                    resolveSaved(ids)
                }
        }
    }

    private fun resolveSaved(ids: List<String>) {
        if (!savedIdsLoaded) return
        savedJob?.cancel()
        savedJob = viewModelScope.launch { resolveSavedNow(ids) }
    }

    private suspend fun resolveSavedNow(ids: List<String>) {
        if (!savedIdsLoaded) return
        val missing = ids.filterNot { it in savedCache }
        if (missing.isNotEmpty()) {
            _state.update { it.copy(saved = it.saved.copy(isLoading = it.saved.items.isEmpty(), error = null)) }
            try {
                val fetched = repository.postsByIds(missing)
                fetched.forEach { savedCache[it.id] = it }
                // Saved ids whose post no longer exists: the author deleted it. Drop them so
                // the list does not keep asking for them
                // the list does not keep asking for them. Own job: each removal makes the
                // listener emit, which cancels this one
                val gone = missing.toSet() - fetched.map { it.id }.toSet()
                if (gone.isNotEmpty()) viewModelScope.launch {
                    gone.forEach { postId ->
                        runCatching { repository.setSaved(postId, saved = false) }
                            .onFailure { Timber.w(it, "Could not drop a saved post that no longer exists") }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not load saved posts")
                _state.update {
                    it.copy(saved = it.saved.copy(isLoading = false, error = "No se pudieron cargar tus guardados"))
                }
                return
            }
        }
        savedCache.keys.retainAll(ids.toSet())
        _state.update { it.copy(saved = TabContent(items = ids.mapNotNull(savedCache::get), isLoading = false)) }
    }

    private fun setSaved(postId: String, saved: Boolean) {
        viewModelScope.launch {
            try {
                repository.setSaved(postId, saved)
                if (!saved) _events.send(ProfileEvent.Unsaved(postId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Could not change a saved post")
                _events.send(ProfileEvent.ShowMessage("No se pudo actualizar. Revisa tu conexión."))
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            preferences.themeMode.collect { mode -> _state.update { it.copy(themeMode = mode) } }
        }
    }

    private fun observePostEvents() {
        viewModelScope.launch {
            eventBus.postChanges.collect { change ->
                when (change) {
                    is PostChange.Deleted -> removePost(change.postId)
                    is PostChange.Edited, is PostChange.Reacted -> {
                        savedCache.remove(change.postId)
                        loadMyPosts()
                        resolveSaved(savedIds)
                    }
                }
            }
        }
        viewModelScope.launch {
            // A post of ours was confirmed by the server: it belongs at the top of the list
            eventBus.events.filter { it.confirmedId != null }.collect { loadMyPosts() }
        }
    }

    private fun removePost(postId: String) {
        savedCache.remove(postId)
        _state.update { state ->
            val posts = state.myPosts.items.filterNot { it.id == postId }
            state.copy(
                myPosts = state.myPosts.copy(items = posts),
                saved = state.saved.copy(items = state.saved.items.filterNot { it.id == postId }),
                stats = ProfileStats.from(posts, isPartial = state.stats.isPartial),
            )
        }
    }
}
