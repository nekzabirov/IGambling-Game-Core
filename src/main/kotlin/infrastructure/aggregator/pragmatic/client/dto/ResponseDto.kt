package infrastructure.aggregator.pragmatic.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResponseDto(
    val error: String = "0",

    val description: String? = null
) {
    val success: Boolean get() = error == "0"
}

@Serializable
data class GamesResponseDto(
    val error: String = "0",

    val description: String? = null,

    val gameList: List<GameDto>? = null
) {
    val success: Boolean get() = error == "0"
}

@Serializable
data class GameUrlResponseDto(
    val error: String = "0",

    val description: String? = null,

    val gameURL: String? = null
) {
    val success: Boolean get() = error == "0"
}
