package application.event

import shared.value.Currency
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Base interface for spin-related events.
 */
@Serializable
sealed interface SpinEvent : DomainEvent {
    val gameIdentity: String
    @Contextual
    val amount: BigInteger
    val currency: Currency
    val playerId: String
    val freeSpinId: String?
}
