package domain.common.event

import shared.value.Currency
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
) : SessionIntegrationEvent {
    override val routingKey: String = "session.opened"
}
