package app.event

import core.value.Currency
import domain.game.model.GameFull
import kotlinx.serialization.Serializable

@Serializable
data class GameWonEvent(
    val game: GameFull,

    val playerId: String,

    val amount: Int,

    val currency: Currency
) : IEvent {
    override val key: String = "game.won"
}
