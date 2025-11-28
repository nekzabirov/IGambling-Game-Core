package com.nekgamebling.domain.game.repository

import com.nekgamebling.domain.game.model.Game
import com.nekgamebling.domain.game.model.GameWithDetails
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import java.util.UUID

/**
 * Repository interface for Game entity operations.
 */
interface GameRepository {
    suspend fun findById(id: UUID): Game?

    suspend fun findByIdentity(identity: String): Game?

    suspend fun findBySymbol(symbol: String): Game?

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
