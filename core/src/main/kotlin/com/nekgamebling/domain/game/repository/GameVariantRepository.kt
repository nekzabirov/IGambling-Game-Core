package com.nekgamebling.domain.game.repository

import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import java.util.UUID

/**
 * Repository interface for GameVariant entity operations.
 */
interface GameVariantRepository {
    suspend fun findById(id: UUID): GameVariant?

    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): GameVariant?

    suspend fun findByGameId(gameId: UUID): List<GameVariant>

    suspend fun findByAggregator(aggregator: Aggregator): List<GameVariant>

    suspend fun save(variant: GameVariant): GameVariant

    suspend fun saveAll(variants: List<GameVariant>): List<GameVariant>

    suspend fun update(variant: GameVariant): GameVariant

    suspend fun delete(id: UUID): Boolean

    suspend fun linkToGame(variantId: UUID, gameId: UUID): Boolean

    suspend fun findAll(pageable: Pageable): Page<GameVariant>
}
