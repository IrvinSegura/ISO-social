package com.nexus.feature.circles.presentation.list

import com.nexus.core.firebase.circle.CircleSummary
import com.nexus.feature.circles.domain.model.InvitePreview

data class CirclesState(
    val circles: List<CircleSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUid: String? = null,
    val isCreating: Boolean = false,
    val join: JoinState = JoinState(),
)

/** The "join with a code" sheet: looking the code up, then confirming the circle it opens. */
data class JoinState(
    val isSearching: Boolean = false,
    val preview: InvitePreview? = null,
    val error: String? = null,
    val isJoining: Boolean = false,
)

sealed interface CirclesIntent {
    data object Retry : CirclesIntent
    data class Create(val name: String, val emoji: String, val colorIndex: Int) : CirclesIntent
    data class LookUpCode(val input: String) : CirclesIntent
    data object ConfirmJoin : CirclesIntent
    data object ResetJoin : CirclesIntent
}

sealed interface CirclesEvent {
    data class ShowMessage(val text: String) : CirclesEvent
    /** A circle was created or joined: the sheet closes and the circle opens. */
    data class OpenCircle(val circleId: String) : CirclesEvent
}
