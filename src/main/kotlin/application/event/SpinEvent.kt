package application.event

import shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Base interface for spin-related events.
 */
@Serializable
sealed interface SpinEvent : DomainEvent {
    val gameIdentity: String
    val amount: Int
    val currency: Currency
    val playerId: String
    val freeSpinId: String?
}
