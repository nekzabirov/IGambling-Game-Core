package infrastructure.aggregator.onegamehub.handler.error

import core.error.IError
import core.error.InsufficientBalanceError
import io.ktor.http.*

sealed class OneGameHubError : Exception() {
    protected abstract val code: String

    protected abstract val display: Boolean

    protected abstract val action: String

    abstract override val message: String

    protected abstract val description: String

    val status = HttpStatusCode.OK

    val body = mapOf(
        "status" to 400,
        "error" to mapOf(
            "code" to code,
            "display" to display,
            "action" to action,
            "message" to message,
            "description" to description
        )
    )

    companion object {
        fun transform(error: Throwable): OneGameHubError {
            if (error !is IError) {
                throw IllegalArgumentException("The error must be an instance of the IError interface.")
            }

            return when (error) {
                is InsufficientBalanceError -> OneGameHubInsufficientBalance()
                else -> throw IllegalArgumentException("Unknown error")
            }
        }
    }
}