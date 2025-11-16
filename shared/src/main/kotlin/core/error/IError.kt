package core.error

sealed class IError(message: String) : Exception(message) {
}