package com.nekgamebling.application.usecase.aggregator

import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable

/**
 * Filter for listing aggregators.
 */
data class AggregatorFilter(
    val query: String = "",
    val active: Boolean? = null,
    val type: Aggregator? = null
) {
    class Builder {
        private var query: String = ""
        private var active: Boolean? = null
        private var type: Aggregator? = null

        fun withQuery(query: String) = apply { this.query = query }
        fun withActive(active: Boolean?) = apply { this.active = active }
        fun withType(type: Aggregator?) = apply { this.type = type }

        fun build() = AggregatorFilter(query, active, type)
    }
}

/**
 * Use case for listing aggregators.
 */
class ListAggregatorUsecase(
    private val aggregatorRepository: AggregatorRepository
) {
    suspend operator fun invoke(
        pageable: Pageable,
        filterBuilder: (AggregatorFilter.Builder) -> Unit = {}
    ): Page<AggregatorInfo> {
        val filter = AggregatorFilter.Builder().also(filterBuilder).build()

        val page = aggregatorRepository.findAll(pageable)

        val filteredItems = page.items.filter { aggregator ->
            val matchesQuery = filter.query.isBlank() ||
                    aggregator.identity.contains(filter.query, ignoreCase = true)
            val matchesActive = filter.active == null || aggregator.active == filter.active
            val matchesType = filter.type == null || aggregator.aggregator == filter.type

            matchesQuery && matchesActive && matchesType
        }

        return Page(
            items = filteredItems,
            totalPages = page.totalPages,
            totalItems = page.totalItems,
            currentPage = page.currentPage
        )
    }
}
