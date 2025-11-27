package com.nekgamebling.application.usecase.collection

import com.nekgamebling.domain.collection.model.Collection
import com.nekgamebling.domain.collection.repository.CollectionRepository
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable

/**
 * Collection list item with game counts.
 */
data class CollectionListItem(
    val category: Collection,
    val totalGamesCount: Int,
    val activeGamesCount: Int
)

/**
 * Filter for listing collections.
 */
data class CollectionFilter(
    val query: String = "",
    val active: Boolean? = null
) {
    class Builder {
        private var query: String = ""
        private var active: Boolean? = null

        fun withQuery(query: String) = apply { this.query = query }
        fun withActive(active: Boolean?) = apply { this.active = active }

        fun build() = CollectionFilter(query, active)
    }
}

/**
 * Use case for listing collections.
 */
class ListCollectionUsecase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(
        pageable: Pageable,
        filterBuilder: (CollectionFilter.Builder) -> Unit = {}
    ): Page<CollectionListItem> {
        val filter = CollectionFilter.Builder().also(filterBuilder).build()

        val page = collectionRepository.findAll(pageable, filter.active ?: false)

        // TODO: Add game counts - for now return zeros
        val items = page.items.map { collection ->
            CollectionListItem(
                category = collection,
                totalGamesCount = 0,
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
