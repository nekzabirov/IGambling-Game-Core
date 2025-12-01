package application.port.outbound

import kotlin.time.Duration

/**
 * Port interface for caching operations.
 */
interface CacheAdapter {
    /**
     * Get a cached value by key.
     */
    suspend fun <T : Any> get(key: String): T?

    /**
     * Save a value to cache with optional TTL.
     */
    suspend fun <T : Any> save(key: String, value: T, ttl: Duration? = null)

    /**
     * Delete a cached value by key.
     */
    suspend fun delete(key: String): Boolean

    /**
     * Check if a key exists in cache.
     */
    suspend fun exists(key: String): Boolean

    /**
     * Clear all cached values.
     */
    suspend fun clear()
}
