package com.nekgamebling.application.event

import kotlinx.serialization.Serializable

/**
 * Event emitted when a session is closed.
 */
@Serializable
data class SessionClosedEvent(
    val sessionId: String,
    val playerId: String,
    val reason: String? = null
) : DomainEvent {
    override val routingKey: String = "session.closed"
}
