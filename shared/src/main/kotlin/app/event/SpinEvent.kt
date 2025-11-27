package app.event

import core.model.SpinType
import core.value.Currency
import domain.game.model.GameFull
import kotlinx.serialization.Serializable

@Serializable
data class SpinEvent(
    val type: SpinType,

    val game: GameFull,

    val amount: Int,

    val currency: Currency,

    val playerId: String,

    val freeSpinId: String?
) : IEvent {
    override val key: String
        get() = "spin." + when (type) {
            SpinType.PLACE -> "place"
            SpinType.SETTLE -> "settle"
            SpinType.ROLLBACK -> "rollback"
        }
}