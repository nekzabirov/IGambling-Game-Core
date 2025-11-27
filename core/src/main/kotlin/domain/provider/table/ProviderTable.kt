package domain.provider.table

import domain.aggregator.table.AggregatorInfoTable
import core.db.AbstractTable
import core.value.ImageMap
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object ProviderTable : AbstractTable("providers") {
    val identity = varchar("identity", 100)

    val name = varchar("name", 100)

    val images = jsonb<ImageMap>("images", Json.Default)
        .default(ImageMap(emptyMap()))

    val order = integer("order")
        .default(100)

    val aggregatorId = reference("aggregator_id", AggregatorInfoTable.id)

    val active = bool("active")
        .default(true)
}