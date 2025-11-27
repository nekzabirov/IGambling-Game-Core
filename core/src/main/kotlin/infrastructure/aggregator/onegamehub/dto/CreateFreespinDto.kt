package infrastructure.aggregator.onegamehub.dto

import infrastructure.aggregator.onegamehub.unit.LocalDateTimeAsStringSerializer
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFreespinDto(
    val id: String,

    @SerialName("start_at")
    @Serializable(with = LocalDateTimeAsStringSerializer::class)
    val startAt: LocalDateTime,

    @SerialName("end_at")
    @Serializable(with = LocalDateTimeAsStringSerializer::class)
    val endAt: LocalDateTime,

    val number: Int,

    @SerialName("player_id")
    val playerId: String,

    val currency: String,

    @SerialName("game_id")
    val gameId: String,

    val bet: Int,

    @SerialName("line_number")
    val lineNumber: Int
)
