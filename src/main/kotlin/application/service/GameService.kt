package application.service

import application.port.outbound.CacheAdapter
import domain.common.error.GameUnavailableError
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.game.repository.GameRepository
import infrastructure.persistence.cache.CachingRepository
import domain.common.value.Aggregator
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Application service for game-related operations.
 * Uses constructor injection for all dependencies.
 */
class GameService(
    private val gameRepository: GameRepository,
    cacheAdapter: CacheAdapter
) {
    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX = "game:"
        private const val CACHE_PREFIX_SYMBOL = "game:symbol:"
    }

    private val detailsCache = CachingRepository<GameWithDetails>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX,
        ttl = CACHE_TTL
    )

    private val gameCache = CachingRepository<Game>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX_SYMBOL,
        ttl = CACHE_TTL
    )

    /**
     * Find game by identity with caching.
     */
    suspend fun findByIdentity(identity: String): Result<GameWithDetails> =
        detailsCache.getOrLoadResult(
            key = identity,
            notFoundError = { NotFoundError("Game", identity) },
            loader = { gameRepository.findWithDetailsByIdentity(identity) }
        )

    /**
     * Find game by ID.
     */
    suspend fun findById(id: UUID): Result<GameWithDetails> =
        detailsCache.getOrLoadResult(
            key = id.toString(),
            notFoundError = { NotFoundError("Game", id.toString()) },
            loader = { gameRepository.findWithDetailsById(id) }
        )

    /**
     * Find game by symbol with caching.
     */
    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Result<Game> =
        gameCache.getOrLoadResult(
            key = "$symbol:aggregator:$aggregator",
            notFoundError = { GameUnavailableError(symbol) },
            loader = { gameRepository.findBySymbol(symbol, aggregator) }
        )

    /**
     * Invalidate cache for a game.
     */
    suspend fun invalidateCache(identity: String) {
        detailsCache.invalidate(identity)
    }
}
