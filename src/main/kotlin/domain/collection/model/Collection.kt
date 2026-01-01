package domain.collection.model

import shared.value.LocaleName
import java.util.UUID

/**
 * Game collection/category entity.
 */
data class Collection(
    val id: UUID = UUID.randomUUID(),
    val identity: String,
    val name: LocaleName,
    val active: Boolean = true,
    val order: Int = 100
) {
    init {
        require(identity.isNotBlank()) { "Collection identity cannot be blank" }
    }

    fun activate(): Collection = copy(active = true)
    fun deactivate(): Collection = copy(active = false)
    fun reorder(newOrder: Int): Collection = copy(order = newOrder)
}

/**
 * Represents the relationship between a game and a collection.
 */
data class CollectionGame(
    val collectionId: UUID,
    val gameId: UUID,
    val order: Int = 0
)
