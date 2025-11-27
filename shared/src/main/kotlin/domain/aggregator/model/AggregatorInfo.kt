package domain.aggregator.model

import core.serializer.UUIDSerializer
import domain.aggregator.model.Aggregator
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AggregatorInfo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,

    val identity: String,

    val config: Map<String, String>,

    val aggregator: Aggregator,

    val active: Boolean = true,
)
