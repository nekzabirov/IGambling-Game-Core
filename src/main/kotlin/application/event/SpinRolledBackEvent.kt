package application.event

import shared.value.Currency
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Event emitted when a spin is rolled back.
 */
@Serializable
data class SpinRolledBackEvent(
    override val gameIdentity: String,
    @Contextual
    override val amount: BigInteger,
    override val currency: Currency,
    override val playerId: String,
    override val freeSpinId: String? = null,
    val reason: String? = null
) : SpinEvent {
    override val routingKey: String = "spin.rolled_back"
}
