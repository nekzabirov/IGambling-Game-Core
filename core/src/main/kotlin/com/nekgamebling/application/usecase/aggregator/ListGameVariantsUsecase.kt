package com.nekgamebling.application.usecase.aggregator

import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.domain.game.repository.GameRepository
import com.nekgamebling.domain.game.repository.GameVariantRepository
import com.nekgamebling.domain.provider.model.Provider
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.ImageMap
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable

/**
 * Game variant list item with game info.
 */
data class GameVariantListItem(
    val gameVariant: GameVariant,
    val game: com.nekgamebling.domain.game.repository.GameListItem
)

/**
 * Filter for game variants.
 */
data class GameVariantFilter(
    val query: String = "",
    val aggregator: Aggregator? = null,
    val gameIdentity: String? = null
) {
    class Builder {
        private var query: String = ""
        private var aggregator: Aggregator? = null
        private var gameIdentity: String? = null

        fun withQuery(query: String) = apply { this.query = query }
        fun withAggregator(aggregator: Aggregator?) = apply { this.aggregator = aggregator }
        fun withGameIdentity(gameIdentity: String?) = apply { this.gameIdentity = gameIdentity }

        fun build() = GameVariantFilter(query, aggregator, gameIdentity)
    }
}

/**
 * Use case for listing game variants.
 */
class ListGameVariantsUsecase(
    private val gameVariantRepository: GameVariantRepository,
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(
        pageable: Pageable,
        filterBuilder: GameVariantFilter.Builder.() -> Unit = {}
    ): Page<GameVariantListItem> {
        val filter = GameVariantFilter.Builder().apply(filterBuilder).build()

        val page = gameVariantRepository.findAll(pageable)

        // TODO: Implement proper filtering
        val items = page.items.mapNotNull { variant ->
            if (filter.aggregator != null && variant.aggregator != filter.aggregator) return@mapNotNull null
            if (filter.query.isNotBlank() && !variant.name.contains(filter.query, ignoreCase = true)) return@mapNotNull null

            val gameId = variant.gameId ?: return@mapNotNull null
            val game = gameRepository.findById(gameId) ?: return@mapNotNull null

            // Create a minimal GameListItem - this is a simplification
            val gameListItem = com.nekgamebling.domain.game.repository.GameListItem(
                game = game,
                variant = variant,
                provider = Provider(
                    id = game.providerId,
                    identity = "",
                    name = variant.providerName,
                    images = ImageMap.EMPTY
                ),
                collections = emptyList()
            )

            GameVariantListItem(variant, gameListItem)
        }

        return Page(
            items = items,
            totalPages = page.totalPages,
            totalItems = page.totalItems,
            currentPage = page.currentPage
        )
    }
}
