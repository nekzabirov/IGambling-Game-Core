package com.nekgamebling.application.usecase.provider

import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository
import com.nekgamebling.domain.provider.model.Provider
import com.nekgamebling.domain.provider.repository.ProviderRepository
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable

/**
 * Provider list item with aggregator info and game counts.
 */
data class ProviderListItem(
    val provider: Provider,
    val aggregatorInfo: AggregatorInfo,
    val totalGamesCount: Int,
    val activeGamesCount: Int
)

/**
 * Filter for listing providers.
 */
data class ProviderFilter(
    val query: String = "",
    val active: Boolean? = null
) {
    class Builder {
        private var query: String = ""
        private var active: Boolean? = null

        fun withQuery(query: String) = apply { this.query = query }
        fun withActive(active: Boolean?) = apply { this.active = active }

        fun build() = ProviderFilter(query, active)
    }
}

/**
 * Use case for listing providers.
 */
class ProviderListUsecase(
    private val providerRepository: ProviderRepository,
    private val aggregatorRepository: AggregatorRepository
) {
    suspend operator fun invoke(
        pageable: Pageable,
        filterBuilder: (ProviderFilter.Builder) -> Unit = {}
    ): Page<ProviderListItem> {
        val filter = ProviderFilter.Builder().also(filterBuilder).build()

        val page = providerRepository.findAll(pageable)

        val items = page.items.mapNotNull { provider ->
            val aggregatorId = provider.aggregatorId ?: return@mapNotNull null
            val aggregator = aggregatorRepository.findById(aggregatorId) ?: return@mapNotNull null

            // Apply filter
            if (filter.active != null && provider.active != filter.active) return@mapNotNull null
            if (filter.query.isNotBlank() && !provider.name.contains(filter.query, ignoreCase = true)) return@mapNotNull null

            ProviderListItem(
                provider = provider,
                aggregatorInfo = aggregator,
                totalGamesCount = 0, // TODO: Add actual count
                activeGamesCount = 0
            )
        }

        return Page(
            items = items,
            totalPages = page.totalPages,
            totalItems = page.totalItems,
            currentPage = page.currentPage
        )
    }
}
