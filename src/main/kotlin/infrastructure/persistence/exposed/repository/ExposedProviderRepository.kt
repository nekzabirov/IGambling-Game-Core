package infrastructure.persistence.exposed.repository

import domain.provider.model.Provider
import domain.provider.repository.ProviderFilter
import domain.provider.repository.ProviderRepository
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.ProviderTable
import shared.value.Page
import shared.value.Pageable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

    override suspend fun findAll(pageable: Pageable, filter: ProviderFilter): Page<Provider> = newSuspendedTransaction {
        val baseQuery = table.selectAll().applyFilters(filter)
        val totalCount = baseQuery.count()

        val items = table.selectAll()
            .applyFilters(filter)
            .orderBy(ProviderTable.order to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toEntity() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal,
        )
    }

    private fun Query.applyFilters(filter: ProviderFilter): Query = apply {
        if (filter.query.isNotBlank()) {
            andWhere {
                ProviderTable.name.ilike(filter.query) or ProviderTable.identity.ilike(filter.query)
            }
        }
        filter.active?.let { active ->
            andWhere { ProviderTable.active eq active }
        }
    }

    override suspend fun assignToAggregator(providerId: UUID, aggregatorId: UUID): Boolean = newSuspendedTransaction {
        ProviderTable.update({ ProviderTable.id eq providerId }) {
            it[ProviderTable.aggregatorId] = aggregatorId
        } > 0
    }

    override suspend fun getGameCountsByProviderIds(providerIds: List<UUID>): Map<UUID, Pair<Int, Int>> {
        if (providerIds.isEmpty()) return emptyMap()

        return newSuspendedTransaction {
            val totalCountExpr = GameTable.id.count()

            // Get total game counts per provider
            val totalCounts = GameTable
                .select(GameTable.providerId, totalCountExpr)
                .where { GameTable.providerId inList providerIds }
                .groupBy(GameTable.providerId)
                .associate { row ->
                    row[GameTable.providerId].value to row[totalCountExpr].toInt()
                }

            // Get active game counts per provider
            val activeCounts = GameTable
                .select(GameTable.providerId, totalCountExpr)
                .where { (GameTable.providerId inList providerIds) and (GameTable.active eq true) }
                .groupBy(GameTable.providerId)
                .associate { row ->
                    row[GameTable.providerId].value to row[totalCountExpr].toInt()
                }

            // Combine into result map
            providerIds.associateWith { providerId ->
                val total = totalCounts[providerId] ?: 0
                val active = activeCounts[providerId] ?: 0
                total to active
            }
        }
    }
}
