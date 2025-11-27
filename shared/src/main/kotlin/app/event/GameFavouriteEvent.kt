package app.event

import domain.game.model.GameFull

data class GameFavouriteEvent(
    val game: GameFull,

    val playerId: String
) : IEvent {
    override val key: String = "game.favourite"
}
