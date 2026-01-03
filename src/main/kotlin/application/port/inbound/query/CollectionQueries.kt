package application.port.inbound.query

import application.port.inbound.Query
import domain.collection.model.Collection
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Read model for collection list items.
 */
data class CollectionListReadModel(
    val id: UUID,
    val identity: String,
    val name: String,
    val order: Int,
    val active: Boolean,
    val gameCount: Int
) {
    companion object {
        fun from(collection: Collection, gameCount: Int = 0) = CollectionListReadModel(
            id = collection.id,
            identity = collection.identity,
            name = collection.name.data.values.firstOrNull() ?: "",
            order = collection.order,
            active = collection.active,
            gameCount = gameCount
        )
    }
}

/**
 * Query to list collections with pagination.
 */
data class ListCollectionsQuery(
    val pageable: Pageable,
    val activeOnly: Boolean = false
) : Query<Page<CollectionListReadModel>>

/**
 * Query to find a collection by identity.
 */
data class FindCollectionByIdentityQuery(
    val identity: String
) : Query<Collection?>

/**
 * Query to find a collection by ID.
 */
data class FindCollectionByIdQuery(
    val id: UUID
) : Query<Collection?>
