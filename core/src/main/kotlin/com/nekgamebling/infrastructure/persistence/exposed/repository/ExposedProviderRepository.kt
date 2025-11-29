package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.provider.model.Provider
import com.nekgamebling.domain.provider.repository.ProviderRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toProvider
import com.nekgamebling.infrastructure.persistence.exposed.table.ProviderTable
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning
import java.util.UUID

/**
 * Exposed implementation of ProviderRepository.
 */
class ExposedProviderRepository : BaseExposedRepositoryWithIdentity<Provider, ProviderTable>(ProviderTable), ProviderRepository {

    override fun ResultRow.toEntity(): Provider = toProvider()

    override suspend fun findByAggregatorId(aggregatorId: UUID): List<Provider> =
        findAllByNullableRef(ProviderTable.aggregatorId, aggregatorId)

    override suspend fun save(provider: Provider): Provider = newSuspendedTransaction {
        val row = ProviderTable.upsertReturning(
            keys = arrayOf(ProviderTable.identity),
            onUpdateExclude = listOf(ProviderTable.name)
        ) {
            it[identity] = provider.identity
            it[name] = provider.name
            it[images] = provider.images
            it[order] = provider.order
            it[aggregatorId] = provider.aggregatorId
            it[active] = provider.active
        }.single()

        provider.copy(
            id = row[ProviderTable.id].value,
            aggregatorId = row[ProviderTable.aggregatorId]?.value,
            active = row[ProviderTable.active],
            order = row[ProviderTable.order],
            name = row[ProviderTable.name],
            images = row[ProviderTable.images]
        )
    }

    override suspend fun update(provider: Provider): Provider = newSuspendedTransaction {
        ProviderTable.update({ ProviderTable.id eq provider.id }) {
            it[identity] = provider.identity
            it[name] = provider.name
            it[images] = provider.images
            it[order] = provider.order
            it[aggregatorId] = provider.aggregatorId
            it[active] = provider.active
        }
        provider
    }

    override suspend fun findAll(pageable: Pageable): Page<Provider> = newSuspendedTransaction {
        val totalCount = table.selectAll().count()
        val items = table.selectAll()
            .orderBy(ProviderTable.order to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toEntity() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    override suspend fun assignToAggregator(providerId: UUID, aggregatorId: UUID): Boolean = newSuspendedTransaction {
        ProviderTable.update({ ProviderTable.id eq providerId }) {
            it[ProviderTable.aggregatorId] = aggregatorId
        } > 0
    }
}
