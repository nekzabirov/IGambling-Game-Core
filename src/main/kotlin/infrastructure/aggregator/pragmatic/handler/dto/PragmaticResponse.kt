package infrastructure.aggregator.pragmatic.handler.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class PragmaticResponse(val error: Int) {
    @Serializable
    data class Success(
        val balance: Long,
        val currency: String,
    ) : PragmaticResponse(0)

    @Serializable
    data class Error(
        val errorCode: String,
        val message: String,
        val description: String,
    ) : PragmaticResponse(1) {
        companion object {
            val PragmaticInvalidRequest = Error(
                errorCode = "ERR001",
                message = "Invalid request",
                description = "Invalid request. Please try again."
            )

            val PragmaticTokenExpired = Error(
                errorCode = "ERR005",
                message = "Session expired",
                description = "Session expired or does not exist"
            )

            val PragmaticInsufficientBalance = Error(
                errorCode = "ERR100",
                message = "Insufficient balance",
                description = "Player does not have sufficient balance"
            )

            val PragmaticGameNotFound = Error(
                errorCode = "ERR200",
                message = "Game not found",
                description = "Requested game was not found"
            )
        }
    }
}
