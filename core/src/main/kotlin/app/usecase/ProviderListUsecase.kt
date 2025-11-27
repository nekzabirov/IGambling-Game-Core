package app.usecase

import core.db.paging
import core.ext.ilike
import core.model.Page
import core.model.Pageable
import domain.aggregator.mapper.toAggregatorModel
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import domain.game.table.GameTable
import domain.provider.mapper.toProvider
import domain.provider.model.Provider
import domain.provider.table.ProviderTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ProviderListUsecase {
    suspend operator fun invoke(pageable: Pageable, filterBuilder: Filter.() -> Unit): Page<ProviderDto> =
        newSuspendedTransaction {
            val filter = Filter().apply(filterBuilder)

            fun Query.applyFilters() = apply {
                if (filter.query.isNotBlank()) {
                    andWhere { ProviderTable.name ilike "%${filter.query}%" or (ProviderTable.identity ilike "%${filter.query}%") }
                }

                filter.active?.also { isActive ->
                    andWhere { ProviderTable.active eq isActive }
                }
            }

            val activeSumExpr = case()
                .When(GameTable.active eq true, intLiteral(1))
                .Else(intLiteral(0))
                .sum()
                .alias("active_sum")

            val totalCountExpr = GameTable.id.count().alias("total_count")

            val aggregators = AggregatorInfoTable
                .selectAll()
                .map { it.toAggregatorModel() }

            ProviderTable
                .leftJoin(GameTable, { ProviderTable.id }, { GameTable.providerId })
                .select(ProviderTable.columns + activeSumExpr + totalCountExpr)
                .applyFilters()
                .groupBy(ProviderTable.id, ProviderTable.name)
                .orderBy(ProviderTable.order to SortOrder.ASC)
                .paging(pageable)
                .map { resultRow ->
                    val provider = resultRow.toProvider()

                    val activeGamesCount = resultRow[activeSumExpr] ?: 0
                    val totalGamesCount = resultRow[totalCountExpr]

                    ProviderDto(
                        provider = provider,
                        activeGamesCount = activeGamesCount,
                        totalGamesCount = totalGamesCount.toInt(),
                        aggregatorInfo = aggregators.find { it.id == provider.aggregatorId }!!
                    )
                }
        }

    class Filter {
        var query: String = ""
            private set

        var active: Boolean? = null
            private set

        fun withQuery(query: String) = apply { this.query = query }

        fun withActive(active: Boolean?) = apply { this.active = active }
    }

    data class ProviderDto(
        val provider: Provider,
        val aggregatorInfo: AggregatorInfo,
        val activeGamesCount: Int,
        val totalGamesCount: Int
    )
}