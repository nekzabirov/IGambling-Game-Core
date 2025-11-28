package com.nekgamebling.application.event

import kotlinx.serialization.Serializable

/**
 * Event emitted when a player adds a game to favorites.
 */
@Serializable
data class GameFavouriteAddedEvent(
    val gameId: String,
    val gameIdentity: String,
    val playerId: String
) : DomainEvent {
    override val routingKey: String = "game.favourite.added"
}
