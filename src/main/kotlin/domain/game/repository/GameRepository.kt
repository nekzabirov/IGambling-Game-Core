package domain.game.repository

import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.common.value.Aggregator
import java.util.UUID

/**
 * Repository interface for Game entity operations.
 * Used by GameService for caching with direct database access.
 */
interface GameRepository {
    /**
     * Find game with all related details (provider, aggregator, variant).
     */
    suspend fun findWithDetailsById(id: UUID): GameWithDetails?

    suspend fun findWithDetailsByIdentity(identity: String): GameWithDetails?

    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Game?
}
