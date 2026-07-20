package uz.cardlens.core.common

interface AppError

sealed interface AppResult<out D, out E : AppError> {
    data class Success<out D>(val data: D) : AppResult<D, Nothing>
    data class Error<out E : AppError>(val error: E) : AppResult<Nothing, E>
}

typealias EmptyResult<E> = AppResult<Unit, E>

inline fun <T, E : AppError, R> AppResult<T, E>.map(transform: (T) -> R): AppResult<R, E> {
    return when (this) {
        is AppResult.Error -> AppResult.Error(error)
        is AppResult.Success -> AppResult.Success(transform(data))
    }
}

inline fun <T, E : AppError> AppResult<T, E>.onSuccess(action: (T) -> Unit): AppResult<T, E> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T, E : AppError> AppResult<T, E>.onFailure(action: (E) -> Unit): AppResult<T, E> {
    if (this is AppResult.Error) action(error)
    return this
}

fun <T, E : AppError> AppResult<T, E>.asEmptyResult(): EmptyResult<E> = map { }

sealed interface DataError : AppError {
    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN
    }

    enum class Network : DataError {
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN,
        NO_INTERNET,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN
    }
}
