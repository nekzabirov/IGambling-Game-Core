package application.service

import application.port.outbound.CacheAdapter
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.repository.AggregatorRepository
import domain.common.error.NotFoundError
import infrastructure.persistence.cache.CachingRepository
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class AggregatorService(
    private val aggregatorRepository: AggregatorRepository,
    cacheAdapter: CacheAdapter
) {
    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX = "aggregator:id:"
    }

    private val cache = CachingRepository<AggregatorInfo>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX,
        ttl = CACHE_TTL
    )

    suspend fun findById(id: UUID): Result<AggregatorInfo> =
        cache.getOrLoadResult(
            key = id.toString(),
            notFoundError = { NotFoundError("Aggregator", id.toString()) },
            loader = { aggregatorRepository.findById(id) }
        )

    /**
     * Invalidate cache for an aggregator.
     */
    suspend fun invalidateCache(id: UUID) {
        cache.invalidate(id.toString())
    }
}