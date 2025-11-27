package app.event

import domain.game.model.GameFull
import kotlinx.serialization.Serializable

@Serializable
data class GameFavouriteEvent(
    val game: GameFull,

    val playerId: String
) : IEvent {
    override val key: String = "game.favourite"
}
