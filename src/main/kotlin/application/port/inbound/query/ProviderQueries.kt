package application.port.inbound.query

import application.port.inbound.Query
import domain.provider.model.Provider
import domain.provider.repository.ProviderFilter
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Read model for provider list items with aggregator info and game counts.
 */
data class ProviderListReadModel(
    val id: UUID,
    val identity: String,
    val name: String,
    val images: Map<String, String>,
    val order: Int,
    val active: Boolean,
    val aggregatorId: UUID?,
    val aggregatorName: String?,
    val totalGameCount: Int,
    val activeGameCount: Int
) {
    companion object {
        fun from(
            provider: Provider,
            aggregatorName: String? = null,
            totalGameCount: Int = 0,
            activeGameCount: Int = 0
        ) = ProviderListReadModel(
            id = provider.id,
            identity = provider.identity,
            name = provider.name,
            images = provider.images.data,
            order = provider.order,
            active = provider.active,
            aggregatorId = provider.aggregatorId,
            aggregatorName = aggregatorName,
            totalGameCount = totalGameCount,
            activeGameCount = activeGameCount
        )
    }
}

/**
 * Query to list providers with pagination and filtering.
 */
data class ListProvidersQuery(
    val pageable: Pageable,
    val filter: ProviderFilter = ProviderFilter()
) : Query<Page<ProviderListReadModel>>

/**
 * Query to find a provider by identity.
 */
data class FindProviderByIdentityQuery(
    val identity: String
) : Query<Provider?>

/**
 * Query to find a provider by ID.
 */
data class FindProviderByIdQuery(
    val id: UUID
) : Query<Provider?>
