package application.port.inbound.query

import application.port.inbound.Query
import domain.aggregator.model.AggregatorInfo
import domain.common.value.Aggregator
import domain.game.model.GameVariant
import domain.game.model.GameVariantWithDetail
import domain.game.repository.GameVariantFilter
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Read model for aggregator list items.
 */
data class AggregatorReadModel(
    val id: UUID,
    val identity: String,
    val aggregator: Aggregator,
    val active: Boolean,
    val config: Map<String, Any>
) {
    companion object {
        fun from(info: AggregatorInfo) = AggregatorReadModel(
            id = info.id,
            identity = info.identity,
            aggregator = info.aggregator,
            active = info.active,
            config = info.config
        )
    }
}

/**
 * Query to list aggregators with pagination and filtering.
 */
data class ListAggregatorsQuery(
    val pageable: Pageable,
    val query: String = "",
    val active: Boolean? = null,
    val type: Aggregator? = null
) : Query<Page<AggregatorReadModel>>

/**
 * Query to list all active aggregators.
 */
data class ListActiveAggregatorsQuery(
    val dummy: Unit = Unit
) : Query<List<AggregatorReadModel>>

/**
 * Query to find an aggregator by identity.
 */
data class FindAggregatorByIdentityQuery(
    val identity: String
) : Query<AggregatorInfo?>

/**
 * Query to find an aggregator by ID.
 */
data class FindAggregatorByIdQuery(
    val id: UUID
) : Query<AggregatorInfo?>

/**
 * Query to find an aggregator by aggregator type.
 */
data class FindAggregatorByTypeQuery(
    val aggregator: Aggregator
) : Query<AggregatorInfo?>

/**
 * Query to list game variants with pagination and filtering.
 */
data class ListGameVariantsQuery(
    val pageable: Pageable,
    val filter: GameVariantFilter = GameVariantFilter.EMPTY
) : Query<Page<GameVariantWithDetail>>
