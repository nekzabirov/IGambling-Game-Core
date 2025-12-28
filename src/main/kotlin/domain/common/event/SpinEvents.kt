package domain.common.event

import shared.serializer.BigIntegerSerializer
import shared.value.Currency
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Event emitted when a spin is placed (bet made).
 */
@Serializable
data class SpinPlacedEvent(
    val gameIdentity: String,
    @Serializable(with = BigIntegerSerializer::class)
    val amount: BigInteger,
    val currency: Currency,
    val playerId: String,
    val freeSpinId: String? = null
) : SpinIntegrationEvent {
    override val routingKey: String = "spin.placed"
}

/**
 * Event emitted when a spin is settled (win/loss determined).
 */
@Serializable
data class SpinSettledEvent(
    val gameIdentity: String,
    @Serializable(with = BigIntegerSerializer::class)
    val amount: BigInteger,
    val currency: Currency,
    val playerId: String,
    val freeSpinId: String? = null,
) : SpinIntegrationEvent {
    override val routingKey: String = "spin.settled"
}

/**
 * Event emitted when a spin ends.
 */
@Serializable
data class SpinEndEvent(
    val gameIdentity: String,
    val playerId: String,
    val freeSpinId: String? = null,
) : SpinIntegrationEvent {
    override val routingKey: String = "spin.end"
}

/**
 * Event emitted when a spin is rolled back (refunded).
 */
@Serializable
data class SpinRollbackEvent(
    val gameIdentity: String,
    val playerId: String,
    @Serializable(with = BigIntegerSerializer::class)
    val refundAmount: BigInteger,
    val currency: Currency,
    val freeSpinId: String? = null,
) : SpinIntegrationEvent {
    override val routingKey: String = "spin.rollback"
}
