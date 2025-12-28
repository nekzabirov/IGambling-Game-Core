package application.query.game

import domain.game.repository.GameFilter
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Query repository for game read operations.
 *
 * This interface separates read concerns from write operations (CQRS-lite).
 * Implementations can optimize for specific query patterns.
 */
interface GameQueryRepository {

    /**
     * Find game details by ID.
     */
    suspend fun findDetailsById(id: UUID): GameDetailsReadModel?

    /**
     * Find game details by identity.
     */
    suspend fun findDetailsByIdentity(identity: String): GameDetailsReadModel?

    /**
     * Find game details by symbol.
     */
    suspend fun findDetailsBySymbol(symbol: String): GameDetailsReadModel?

    /**
     * Find games with pagination and filtering.
     */
    suspend fun findAll(pageable: Pageable, filter: GameFilter): Page<GameListReadModel>

    /**
     * Search games by name or identity.
     */
    suspend fun search(query: String, limit: Int = 20): List<GameSummaryReadModel>
}
