package com.nexus.core.common.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult

fun <T> Flow<T>.asResult(): Flow<NexusResult<T>> =
    map<T, NexusResult<T>> { NexusResult.Success(it) }
        .catch { emit(NexusResult.Error(NexusException.UnknownException(it))) }
