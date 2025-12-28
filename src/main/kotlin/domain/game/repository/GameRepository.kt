package domain.game.repository

import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.common.value.Aggregator
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Repository interface for Game entity operations.
 */
interface GameRepository {
    suspend fun findById(id: UUID): Game?

    suspend fun findByIdentity(identity: String): Game?

    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Game?

    suspend fun findByNameAndProviderId(name: String, providerId: UUID): Game?

    suspend fun save(game: Game): Game

    suspend fun update(game: Game): Game

    suspend fun delete(id: UUID): Boolean

    suspend fun existsByIdentity(identity: String): Boolean

    /**
     * Find game with all related details (provider, aggregator, variant).
     */
    suspend fun findWithDetailsById(id: UUID): GameWithDetails?

    suspend fun findWithDetailsByIdentity(identity: String): GameWithDetails?

    suspend fun findWithDetailsBySymbol(symbol: String): GameWithDetails?

    /**
     * List games with filtering and pagination.
     */
    suspend fun findAll(pageable: Pageable, filter: GameFilter): Page<GameListItem>

    /**
     * Tag management.
     */
    suspend fun addTag(gameId: UUID, tag: String): Boolean
    suspend fun removeTag(gameId: UUID, tag: String): Boolean
}
