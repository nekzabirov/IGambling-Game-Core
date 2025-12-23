package application.event

import kotlinx.serialization.Serializable

/**
 * Event emitted when a spin ends.
 */
@Serializable
data class SpinEndEvent(
    val gameIdentity: String,
    val playerId: String,
    val freeSpinId: String? = null,
) : DomainEvent {
    override val routingKey: String = "spin.end"
}
