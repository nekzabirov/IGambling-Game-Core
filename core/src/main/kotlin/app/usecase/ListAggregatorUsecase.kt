package app.usecase

import domain.aggregator.mapper.toAggregatorModel
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import core.db.paging
import domain.aggregator.model.Aggregator
import core.model.Page
import core.model.Pageable
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ListAggregatorUsecase {
    suspend operator fun invoke(pageable: Pageable, filterBuilder: (Filter) -> Unit): Page<AggregatorInfo> =
        newSuspendedTransaction {
            val filter = Filter().apply(filterBuilder)

            AggregatorInfoTable.selectAll()
                .apply {
                    if (filter.query.isNotBlank())
                        andWhere { AggregatorInfoTable.identity like "%${filter.query}%" }

                    filter.type?.let {
                        andWhere { AggregatorInfoTable.aggregator eq it }
                    }

                    filter.active?.let {
                        andWhere { AggregatorInfoTable.active eq it }
                    }
                }
                .paging(pageable)
                .map { it.toAggregatorModel() }
        }

    class Filter {
        var query: String = ""
            private set

        var active: Boolean? = null
            private set

        var type: Aggregator? = null
            private set

        fun withQuery(query: String) = apply { this.query = query }

        fun withActive(active: Boolean) = apply { this.active = active }

        fun withType(type: Aggregator) = apply { this.type = type }
    }
}