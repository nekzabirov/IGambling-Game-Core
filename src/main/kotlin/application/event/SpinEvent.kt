package application.event

import shared.serializer.BigIntegerSerializer
import shared.value.Currency
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Base interface for spin-related events.
 */
@Serializable
sealed interface SpinEvent : DomainEvent {
    val gameIdentity: String
    @Serializable(with = BigIntegerSerializer::class)
    val amount: BigInteger
    val currency: Currency
    val playerId: String
    val freeSpinId: String?
}
