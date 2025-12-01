package application.service

import application.port.outbound.CacheAdapter
import domain.common.error.GameUnavailableError
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.game.repository.GameRepository
import shared.value.Aggregator
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Application service for game-related operations.
 * Uses constructor injection for all dependencies.
 */
class GameService(
    private val gameRepository: GameRepository,
    private val cacheAdapter: CacheAdapter
) {
    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX = "game:"
    }

    /**
     * Find game by identity with caching.
     */
    suspend fun findByIdentity(identity: String): Result<GameWithDetails> {
        // Check cache first
        cacheAdapter.get<GameWithDetails>("$CACHE_PREFIX$identity")?.let {
            return Result.success(it)
        }

        val game = gameRepository.findWithDetailsByIdentity(identity)
            ?: return Result.failure(NotFoundError("Game", identity))

        // Cache the result
        cacheAdapter.save("$CACHE_PREFIX$identity", game, CACHE_TTL)

        return Result.success(game)
    }

    /**
     * Find game by ID.
     */
    suspend fun findById(id: UUID): Result<GameWithDetails> {
        val cacheKey = "$CACHE_PREFIX$id"

        cacheAdapter.get<GameWithDetails>(cacheKey)?.let {
            return Result.success(it)
        }

        val game = gameRepository.findWithDetailsById(id)
            ?: return Result.failure(NotFoundError("Game", id.toString()))

        cacheAdapter.save(cacheKey, game, CACHE_TTL)

        return Result.success(game)
    }

    /**
     * Find game by symbol with caching.
     */
    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Result<Game> {
        val cacheKey = "${CACHE_PREFIX}symbol:$symbol:aggregator:$aggregator"

        cacheAdapter.get<Game>(cacheKey)?.let {
            return Result.success(it)
        }

        val game = gameRepository.findBySymbol(symbol, aggregator)
            ?: return Result.failure(GameUnavailableError(symbol))

        cacheAdapter.save(cacheKey, game, CACHE_TTL)

        return Result.success(game)
    }

    /**
     * Invalidate cache for a game.
     */
    suspend fun invalidateCache(identity: String) {
        cacheAdapter.delete("$CACHE_PREFIX$identity")
    }
}
