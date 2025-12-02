package infrastructure.aggregator.pragmatic.client.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CancelFreespinDto(
    @SerialName("externalBonusId")
    val externalBonusId: String
)
