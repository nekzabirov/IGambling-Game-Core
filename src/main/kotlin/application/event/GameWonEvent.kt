package application.event

import shared.value.Currency
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Event emitted when a player wins a game.
 */
@Serializable
data class GameWonEvent(
    val gameId: String,
    val gameIdentity: String,
    val playerId: String,
    @Contextual
    val amount: BigInteger,
    val currency: Currency
) : DomainEvent {
    override val routingKey: String = "game.won"
}
