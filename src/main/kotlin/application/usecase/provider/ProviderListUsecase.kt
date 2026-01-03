package application.usecase.provider

import domain.aggregator.model.AggregatorInfo
import domain.aggregator.repository.AggregatorRepository
import domain.provider.model.Provider
import domain.provider.repository.ProviderFilter
import domain.provider.repository.ProviderRepository
import shared.value.Page
import shared.value.Pageable

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
 * Use case for listing providers.
 */
class ProviderListUsecase(
    private val providerRepository: ProviderRepository,
    private val aggregatorRepository: AggregatorRepository
) {
    suspend operator fun invoke(
        pageable: Pageable,
        filter: ProviderFilter = ProviderFilter()
    ): Page<ProviderListItem> {
        val page = providerRepository.findAll(pageable, filter)

        // Batch load aggregator IDs to avoid N+1
        val aggregatorIds = page.items.mapNotNull { it.aggregatorId }.distinct()
        val aggregatorsMap = aggregatorIds.associateWith { id ->
            aggregatorRepository.findById(id)
        }

        // Batch load game counts to avoid N+1
        val providerIds = page.items.map { it.id }
        val gameCountsMap = providerRepository.getGameCountsByProviderIds(providerIds)

        val items = page.items.mapNotNull { provider ->
            val aggregatorId = provider.aggregatorId ?: return@mapNotNull null
            val aggregator = aggregatorsMap[aggregatorId] ?: return@mapNotNull null
            val (totalGames, activeGames) = gameCountsMap[provider.id] ?: (0 to 0)

            ProviderListItem(
                provider = provider,
                aggregatorInfo = aggregator,
                totalGamesCount = totalGames,
                activeGamesCount = activeGames
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
