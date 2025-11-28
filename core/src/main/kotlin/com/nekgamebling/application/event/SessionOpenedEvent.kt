package com.nekgamebling.application.event

import com.nekgamebling.shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Event emitted when a new session is opened.
 */
@Serializable
data class SessionOpenedEvent(
    val sessionId: String,
    val gameId: String,
    val gameIdentity: String,
    val playerId: String,
    val currency: Currency,
    val platform: String
) : DomainEvent {
    override val routingKey: String = "session.opened"
}
