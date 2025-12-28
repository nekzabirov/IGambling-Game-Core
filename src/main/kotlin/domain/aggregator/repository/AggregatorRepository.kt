package domain.aggregator.repository

import domain.aggregator.model.AggregatorInfo
import domain.common.value.Aggregator
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Repository interface for AggregatorInfo entity operations.
 */
interface AggregatorRepository {
    suspend fun findById(id: UUID): AggregatorInfo?
    suspend fun findByIdentity(identity: String): AggregatorInfo?
    suspend fun findByAggregator(aggregator: Aggregator): AggregatorInfo?
    suspend fun save(aggregatorInfo: AggregatorInfo): AggregatorInfo
    suspend fun update(aggregatorInfo: AggregatorInfo): AggregatorInfo
    suspend fun delete(id: UUID): Boolean
    suspend fun existsByIdentity(identity: String): Boolean
    suspend fun findAll(pageable: Pageable): Page<AggregatorInfo>
    suspend fun findAllActive(): List<AggregatorInfo>
}
