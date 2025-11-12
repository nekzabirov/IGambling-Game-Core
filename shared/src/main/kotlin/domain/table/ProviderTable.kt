package domain.table

import domain.table.base.AbstractTable
import domain.value.ImageMap
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object ProviderTable : domain.table.base.AbstractTable("providers") {
    val identity = varchar("identity", 100)

    val name = varchar("name", 100)

    val images = jsonb<domain.value.ImageMap>("images", Json.Default)
        .default(_root_ide_package_.domain.value.ImageMap(emptyMap()))

    val order = integer("order")
        .default(100)

    val aggregatorId = reference("aggregator_id", AggregatorInfoTable.id)

    val active = bool("active")
        .default(true)
}