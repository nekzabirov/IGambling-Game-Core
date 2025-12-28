package infrastructure.persistence.cache

import application.port.outbound.CacheAdapter
import kotlin.time.Duration

/**
 * Generic caching decorator for repository operations.
 * Implements read-through caching pattern to reduce code duplication.
 *
 * Usage:
 * ```kotlin
 * class GameService(
 *     private val gameRepository: GameRepository,
 *     cacheAdapter: CacheAdapter
 * ) {
 *     private val cache = CachingRepository<GameWithDetails>(
 *         cacheAdapter = cacheAdapter,
 *         cachePrefix = "game:",
 *         ttl = 5.minutes
 *     )
 *
 *     suspend fun findByIdentity(identity: String): Result<GameWithDetails> =
 *         cache.getOrLoadResult(
 *             key = identity,
 *             notFoundError = { NotFoundError("Game", identity) },
 *             loader = { gameRepository.findWithDetailsByIdentity(identity) }
 *         )
 * }
 * ```
 */
class CachingRepository<T : Any>(
    private val cacheAdapter: CacheAdapter,
    private val cachePrefix: String,
    private val ttl: Duration
) {
    /**
     * Get from cache or load from source, caching the result.
     * Returns null if not found in cache and loader returns null.
     *
     * @param key Cache key (will be prefixed with cachePrefix)
     * @param loader Function to load the value if not in cache
     * @return Cached or loaded value, or null if not found
     */
    suspend fun getOrLoad(
        key: String,
        loader: suspend () -> T?
    ): T? {
        val cacheKey = "$cachePrefix$key"

        // Try cache first
        cacheAdapter.get<T>(cacheKey)?.let { return it }

        // Load from source
        val value = loader() ?: return null

        // Cache the result
        cacheAdapter.save(cacheKey, value, ttl)

        return value
    }

    /**
     * Get from cache or load, returning Result.
     * Returns failure with provided error if not found.
     *
     * @param key Cache key (will be prefixed with cachePrefix)
     * @param notFoundError Function to create error when not found
     * @param loader Function to load the value if not in cache
     * @return Result with value or error
     */
    suspend inline fun <reified E : Throwable> getOrLoadResult(
        key: String,
        notFoundError: () -> E,
        crossinline loader: suspend () -> T?
    ): Result<T> {
        val value = getOrLoad(key) { loader() }
        return if (value != null) {
            Result.success(value)
        } else {
            Result.failure(notFoundError())
        }
    }

    /**
     * Invalidate a cache entry.
     *
     * @param key Cache key (will be prefixed with cachePrefix)
     */
    suspend fun invalidate(key: String) {
        cacheAdapter.delete("$cachePrefix$key")
    }

    /**
     * Invalidate multiple cache entries.
     *
     * @param keys Cache keys to invalidate
     */
    suspend fun invalidateAll(vararg keys: String) {
        keys.forEach { invalidate(it) }
    }

    /**
     * Multi-get with cache-aside pattern.
     * Efficiently loads multiple items, hitting cache first and batching misses.
     *
     * @param keys List of cache keys
     * @param loader Function to load missing values by their keys
     * @return Map of key to value for all found items
     */
    suspend fun getOrLoadMany(
        keys: List<String>,
        loader: suspend (List<String>) -> Map<String, T>
    ): Map<String, T> {
        if (keys.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, T>()
        val missingKeys = mutableListOf<String>()

        // Check cache for each key
        keys.forEach { key ->
            val cached = cacheAdapter.get<T>("$cachePrefix$key")
            if (cached != null) {
                result[key] = cached
            } else {
                missingKeys.add(key)
            }
        }

        // Batch load missing keys
        if (missingKeys.isNotEmpty()) {
            val loaded = loader(missingKeys)
            loaded.forEach { (key, value) ->
                cacheAdapter.save("$cachePrefix$key", value, ttl)
                result[key] = value
            }
        }

        return result
    }

    /**
     * Update cache with a new value.
     * Useful when you know the value has changed and want to update cache proactively.
     *
     * @param key Cache key
     * @param value New value to cache
     */
    suspend fun update(key: String, value: T) {
        cacheAdapter.save("$cachePrefix$key", value, ttl)
    }

    /**
     * Check if a key exists in cache.
     *
     * @param key Cache key
     * @return true if cached
     */
    suspend fun exists(key: String): Boolean {
        return cacheAdapter.exists("$cachePrefix$key")
    }
}

/**
 * Extension to create a CachingRepository with a shorter syntax.
 */
fun <T : Any> CacheAdapter.cached(
    prefix: String,
    ttl: Duration
): CachingRepository<T> = CachingRepository(this, prefix, ttl)
