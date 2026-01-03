package infrastructure.handler.query

import application.port.inbound.QueryHandler
import application.port.inbound.query.*
import domain.aggregator.model.AggregatorInfo
import domain.common.value.Aggregator
import domain.game.model.GameVariantWithDetail
import domain.game.repository.GameVariantFilter
import infrastructure.persistence.exposed.ilike
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.mapper.toGame
import infrastructure.persistence.exposed.mapper.toGameVariant
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameVariantTable
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

/**
 * Query handler for listing aggregators with pagination and filtering.
 */
class ListAggregatorsQueryHandler : QueryHandler<ListAggregatorsQuery, Page<AggregatorReadModel>> {
    override suspend fun handle(query: ListAggregatorsQuery): Page<AggregatorReadModel> = newSuspendedTransaction {
        val pageable = query.pageable

        var baseQuery = AggregatorInfoTable.selectAll()

        if (query.query.isNotBlank()) {
            baseQuery = baseQuery.andWhere {
                AggregatorInfoTable.identity.ilike(query.query)
            }
        }

        query.active?.let { active ->
            baseQuery = baseQuery.andWhere { AggregatorInfoTable.active eq active }
        }

        query.type?.let { type ->
            baseQuery = baseQuery.andWhere { AggregatorInfoTable.aggregator eq type }
        }

        val totalCount = baseQuery.count()

        val items = baseQuery
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { AggregatorReadModel.from(it.toAggregatorInfo()) }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }
}

/**
 * Query handler for listing all active aggregators.
 */
class ListActiveAggregatorsQueryHandler : QueryHandler<ListActiveAggregatorsQuery, List<AggregatorReadModel>> {
    override suspend fun handle(query: ListActiveAggregatorsQuery): List<AggregatorReadModel> = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .where { AggregatorInfoTable.active eq true }
            .map { AggregatorReadModel.from(it.toAggregatorInfo()) }
    }
}

/**
 * Query handler for finding an aggregator by identity.
 */
class FindAggregatorByIdentityQueryHandler : QueryHandler<FindAggregatorByIdentityQuery, AggregatorInfo?> {
    override suspend fun handle(query: FindAggregatorByIdentityQuery): AggregatorInfo? = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .where { AggregatorInfoTable.identity eq query.identity }
            .singleOrNull()
            ?.toAggregatorInfo()
    }
}

/**
 * Query handler for finding an aggregator by ID.
 */
class FindAggregatorByIdQueryHandler : QueryHandler<FindAggregatorByIdQuery, AggregatorInfo?> {
    override suspend fun handle(query: FindAggregatorByIdQuery): AggregatorInfo? = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .where { AggregatorInfoTable.id eq query.id }
            .singleOrNull()
            ?.toAggregatorInfo()
    }
}

/**
 * Query handler for finding an aggregator by type.
 */
class FindAggregatorByTypeQueryHandler : QueryHandler<FindAggregatorByTypeQuery, AggregatorInfo?> {
    override suspend fun handle(query: FindAggregatorByTypeQuery): AggregatorInfo? = newSuspendedTransaction {
        AggregatorInfoTable.selectAll()
            .where { AggregatorInfoTable.aggregator eq query.aggregator }
            .singleOrNull()
            ?.toAggregatorInfo()
    }
}

/**
 * Query handler for listing game variants with pagination and filtering.
 */
class ListGameVariantsQueryHandler : QueryHandler<ListGameVariantsQuery, Page<GameVariantWithDetail>> {
    override suspend fun handle(query: ListGameVariantsQuery): Page<GameVariantWithDetail> = newSuspendedTransaction {
        val pageable = query.pageable
        val filter = query.filter

        val baseQuery = GameVariantTable
            .leftJoin(GameTable, onColumn = { GameVariantTable.gameId }, otherColumn = { GameTable.id })
            .leftJoin(ProviderTable, onColumn = { GameTable.providerId }, otherColumn = { ProviderTable.id })
            .selectAll()
            .applyFilters(filter)

        val totalCount = baseQuery.count()

        val items = baseQuery
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map {
                GameVariantWithDetail(
                    variant = it.toGameVariant(),
                    game = if (it.getOrNull(GameTable.id) != null) it.toGame() else null,
                    provider = if (it.getOrNull(ProviderTable.id) != null) it.toProvider() else null
                )
            }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    private fun Query.applyFilters(filter: GameVariantFilter): Query = apply {
        if (filter.query.isNotBlank()) {
            andWhere {
                GameVariantTable.name.ilike(filter.query) or
                        GameVariantTable.providerName.ilike(filter.query) or
                        GameVariantTable.symbol.ilike(filter.query)
            }
        }

        filter.aggregator?.let { aggregator ->
            andWhere { GameVariantTable.aggregator eq aggregator }
        }

        filter.gameIdentity?.let { gameIdentity ->
            andWhere { GameTable.identity eq gameIdentity }
        }
    }
}
