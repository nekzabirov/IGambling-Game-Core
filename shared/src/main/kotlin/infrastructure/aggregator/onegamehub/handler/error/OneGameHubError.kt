package infrastructure.aggregator.onegamehub.handler.error

import core.error.IError
import core.error.InsufficientBalanceError
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class OneGameHubErrorDetails(
    val code: String,
    val display: Boolean,
    val action: String,
    val message: String,
    val description: String,
)

@Serializable
data class OneGameHubErrorBody(
    val status: Int = 400,
    val error: OneGameHubErrorDetails,
)

sealed class OneGameHubError : Exception() {
    protected abstract val code: String
    protected abstract val display: Boolean
    protected abstract val action: String
    abstract override val message: String
    protected abstract val description: String

    val status = HttpStatusCode.OK

    val body: OneGameHubErrorBody
        get() = OneGameHubErrorBody(
            error = OneGameHubErrorDetails(
                code = code,
                display = display,
                action = action,
                message = message,
                description = description
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