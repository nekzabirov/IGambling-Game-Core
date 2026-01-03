package infrastructure.handler.query

import application.port.inbound.QueryHandler
import application.port.inbound.query.*
import domain.provider.model.Provider
import domain.provider.repository.ProviderFilter
import infrastructure.persistence.exposed.ilike
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page
import java.util.UUID

/**
 * Query handler for listing providers with pagination and filtering.
 * Includes batch loading of aggregator names and game counts.
 */
class ListProvidersQueryHandler : QueryHandler<ListProvidersQuery, Page<ProviderListReadModel>> {
    override suspend fun handle(query: ListProvidersQuery): Page<ProviderListReadModel> = newSuspendedTransaction {
        val pageable = query.pageable
        val filter = query.filter

        val baseQuery = ProviderTable.selectAll().applyFilters(filter)
        val totalCount = baseQuery.count()

        val providers = ProviderTable.selectAll()
            .applyFilters(filter)
            .orderBy(ProviderTable.order to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toProvider() }

        val providerIds = providers.map { it.id }

        // Batch load aggregator names
        val aggregatorIds = providers.mapNotNull { it.aggregatorId }.distinct()
        val aggregatorNames = if (aggregatorIds.isNotEmpty()) {
            AggregatorInfoTable.selectAll()
                .where { AggregatorInfoTable.id inList aggregatorIds }
                .associate { row ->
                    row[AggregatorInfoTable.id].value to row[AggregatorInfoTable.aggregator].name
                }
        } else {
            emptyMap()
        }

        // Batch load game counts
        val gameCounts = getGameCountsByProviderIds(providerIds)

        val items = providers.map { provider ->
            val counts = gameCounts[provider.id] ?: (0 to 0)
            ProviderListReadModel.from(
                provider = provider,
                aggregatorName = provider.aggregatorId?.let { aggregatorNames[it] },
                totalGameCount = counts.first,
                activeGameCount = counts.second
            )
        }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
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

    private fun getGameCountsByProviderIds(providerIds: List<UUID>): Map<UUID, Pair<Int, Int>> {
        if (providerIds.isEmpty()) return emptyMap()

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
        return providerIds.associateWith { providerId ->
            val total = totalCounts[providerId] ?: 0
            val active = activeCounts[providerId] ?: 0
            total to active
        }
    }
}

/**
 * Query handler for finding a provider by identity.
 */
class FindProviderByIdentityQueryHandler : QueryHandler<FindProviderByIdentityQuery, Provider?> {
    override suspend fun handle(query: FindProviderByIdentityQuery): Provider? = newSuspendedTransaction {
        ProviderTable.selectAll()
            .where { ProviderTable.identity eq query.identity }
            .singleOrNull()
            ?.toProvider()
    }
}

/**
 * Query handler for finding a provider by ID.
 */
class FindProviderByIdQueryHandler : QueryHandler<FindProviderByIdQuery, Provider?> {
    override suspend fun handle(query: FindProviderByIdQuery): Provider? = newSuspendedTransaction {
        ProviderTable.selectAll()
            .where { ProviderTable.id eq query.id }
            .singleOrNull()
            ?.toProvider()
    }
}
