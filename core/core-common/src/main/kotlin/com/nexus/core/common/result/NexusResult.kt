package com.nexus.core.common.result

sealed interface NexusResult<out T> {
    data class Success<T>(val data: T) : NexusResult<T>
    data class Error(val exception: NexusException) : NexusResult<Nothing>
}

sealed class NexusException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class NetworkException(
        val code: Int? = null,
        message: String = "Network error. Check your connection.",
    ) : NexusException(message)

    class InvalidCredentialsException :
        NexusException("Invalid email or password.")

    class EmailAlreadyInUseException :
        NexusException("This email is already registered.")

    class WeakPasswordException :
        NexusException("Password must be at least 8 characters.")

    class UserNotFoundException :
        NexusException("User account not found.")

    class UnknownException(cause: Throwable? = null) :
        NexusException("An unexpected error occurred.", cause)
}

inline fun <T> NexusResult<T>.onSuccess(action: (T) -> Unit): NexusResult<T> {
    if (this is NexusResult.Success) action(data)
    return this
}

inline fun <T> NexusResult<T>.onError(action: (NexusException) -> Unit): NexusResult<T> {
    if (this is NexusResult.Error) action(exception)
    return this
}

inline fun <T, R> NexusResult<T>.map(transform: (T) -> R): NexusResult<R> = when (this) {
    is NexusResult.Success -> NexusResult.Success(transform(data))
    is NexusResult.Error -> this
}

suspend fun <T> safeCall(block: suspend () -> T): NexusResult<T> = try {
    NexusResult.Success(block())
} catch (e: NexusException) {
    NexusResult.Error(e)
} catch (e: Exception) {
    NexusResult.Error(NexusException.UnknownException(e))
}
