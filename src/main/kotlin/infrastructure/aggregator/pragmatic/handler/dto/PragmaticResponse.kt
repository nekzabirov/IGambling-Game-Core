package infrastructure.aggregator.pragmatic.handler.dto

import kotlinx.serialization.Serializable

sealed interface PragmaticResponse {
    @Serializable
    data class Success(
        val cash: String,
        val bonus: String,
        val currency: String,

        val userId: String? = null,

        val transactionId: String? = null,

        val usedPromo: String? = null
    ) : PragmaticResponse
}
