package com.nekgamebling.application.service

import application.port.outbound.CacheAdapter
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.repository.AggregatorRepository
import domain.common.error.NotFoundError
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class AggregatorService(
    private val aggregatorRepository: AggregatorRepository,
    private val cacheAdapter: CacheAdapter
) {

    suspend fun findById(id: UUID): Result<AggregatorInfo> {
        // Check cache first
        cacheAdapter.get<AggregatorInfo>("${CACHE_PREFIX}id:$id")?.let {
            return Result.success(it)
        }

        val aggregatorInfo = aggregatorRepository.findById(id)
            ?: return Result.failure(NotFoundError("Aggregator", id.toString()))

        cacheAdapter.save("${CACHE_PREFIX}id:$id", aggregatorInfo, CACHE_TTL)

        return Result.success(aggregatorInfo)
    }

    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX = "aggregator:"
    }
}