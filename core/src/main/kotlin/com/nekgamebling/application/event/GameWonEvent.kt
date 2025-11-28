package com.nekgamebling.application.event

import com.nekgamebling.shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Event emitted when a player wins a game.
 */
@Serializable
data class GameWonEvent(
    val gameId: String,
    val gameIdentity: String,
    val playerId: String,
    val amount: Int,
    val currency: Currency
) : DomainEvent {
    override val routingKey: String = "game.won"
}
