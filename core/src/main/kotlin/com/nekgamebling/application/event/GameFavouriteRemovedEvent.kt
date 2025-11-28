package com.nekgamebling.application.event

import kotlinx.serialization.Serializable

/**
 * Event emitted when a player removes a game from favorites.
 */
@Serializable
data class GameFavouriteRemovedEvent(
    val gameId: String,
    val gameIdentity: String,
    val playerId: String
) : DomainEvent {
    override val routingKey: String = "game.favourite.removed"
}
