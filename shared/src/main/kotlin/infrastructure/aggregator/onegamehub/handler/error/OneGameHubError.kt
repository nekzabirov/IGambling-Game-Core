package infrastructure.aggregator.onegamehub.handler.error

import io.ktor.http.HttpStatusCode

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
}