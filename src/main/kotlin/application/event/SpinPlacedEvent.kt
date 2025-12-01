package application.event

import shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Event emitted when a spin is placed (bet made).
 */
@Serializable
data class SpinPlacedEvent(
    override val gameIdentity: String,
    override val amount: Int,
    override val currency: Currency,
    override val playerId: String,
    override val freeSpinId: String? = null
) : SpinEvent {
    override val routingKey: String = "spin.placed"
}
