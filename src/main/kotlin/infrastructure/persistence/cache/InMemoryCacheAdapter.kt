package infrastructure.persistence.cache

import application.port.outbound.CacheAdapter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Thread-safe in-memory cache implementation.
 */
class InMemoryCacheAdapter : CacheAdapter {
    private data class CacheEntry(
        val value: Any,
        val expiresAt: Long?
    ) {
        fun isExpired(): Boolean = expiresAt != null && System.currentTimeMillis() > expiresAt
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> get(key: String): T? {
        val entry = cache[key] ?: return null

        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }

        return entry.value as? T
    }

    override suspend fun <T : Any> save(key: String, value: T, ttl: Duration?) {
        val expiresAt = ttl?.let { System.currentTimeMillis() + it.inWholeMilliseconds }
        cache[key] = CacheEntry(value, expiresAt)
    }

    override suspend fun delete(key: String): Boolean {
        return cache.remove(key) != null
    }

    override suspend fun exists(key: String): Boolean {
        val entry = cache[key] ?: return false

        if (entry.isExpired()) {
            cache.remove(key)
            return false
        }

        return true
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }

    /**
     * Remove all expired entries.
     */
    suspend fun cleanup() {
        mutex.withLock {
            val expiredKeys = cache.entries
                .filter { it.value.isExpired() }
                .map { it.key }

            expiredKeys.forEach { cache.remove(it) }
        }
    }
}
