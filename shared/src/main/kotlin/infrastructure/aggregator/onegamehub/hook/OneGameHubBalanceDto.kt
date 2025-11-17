package infrastructure.aggregator.onegamehub.hook

import kotlinx.serialization.Serializable

@Serializable
data class OneGameHubBalanceDto(
    val status: Int = 200,
    val balance: Int,
    val currency: String,
)
