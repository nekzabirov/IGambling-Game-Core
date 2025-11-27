package app.event

import core.model.SpinType
import core.value.Currency
import domain.game.model.GameFull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class SpinEvent(
    @SerialName("spinType")
    val type: SpinType,

    val game: GameFull,

    val amount: Int,

    val currency: Currency,

    val playerId: String,

    val freeSpinId: String?
) : IEvent {
    override val key: String
        get() = "spin." + when (type) {
            SpinType.PLACE -> "placed"
            SpinType.SETTLE -> "settled"
            SpinType.ROLLBACK -> "rolled_back"
        }
}