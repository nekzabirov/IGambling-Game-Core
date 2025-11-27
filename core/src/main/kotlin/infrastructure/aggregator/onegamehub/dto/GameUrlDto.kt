package infrastructure.aggregator.onegamehub.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameUrlDto(
    @SerialName("game_url")
    val gameUrl: String = "",

    val token: String = ""
)
