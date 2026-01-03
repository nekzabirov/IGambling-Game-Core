package domain.provider.repository

import domain.provider.model.Provider
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Repository interface for Provider entity operations.
 */
interface ProviderRepository {
    suspend fun findById(id: UUID): Provider?

    suspend fun findByIdentity(identity: String): Provider?

    suspend fun findByAggregatorId(aggregatorId: UUID): List<Provider>

    suspend fun save(provider: Provider): Provider

    suspend fun update(provider: Provider): Provider

    suspend fun delete(id: UUID): Boolean

    suspend fun existsByIdentity(identity: String): Boolean

    suspend fun findAll(pageable: Pageable, filter: ProviderFilter = ProviderFilter()): Page<Provider>

    /**
     * Assign provider to an aggregator.
     */
    suspend fun assignToAggregator(providerId: UUID, aggregatorId: UUID): Boolean
}
