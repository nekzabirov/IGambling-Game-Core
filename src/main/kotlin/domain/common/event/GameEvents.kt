package domain.common.event

import shared.value.Currency
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Event emitted when a player adds a game to favorites.
 */
@Serializable
data class GameFavouriteAddedEvent(
    val gameId: String,
    val gameIdentity: String,
    val playerId: String
) : GameIntegrationEvent {
    override val routingKey: String = "game.favourite.added"
}

/**
 * Event emitted when a player removes a game from favorites.
 */
@Serializable
data class GameFavouriteRemovedEvent(
    val gameId: String,
    val gameIdentity: String,
    val playerId: String
) : GameIntegrationEvent {
    override val routingKey: String = "game.favourite.removed"
}

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
) : GameIntegrationEvent {
    override val routingKey: String = "game.won"
}
