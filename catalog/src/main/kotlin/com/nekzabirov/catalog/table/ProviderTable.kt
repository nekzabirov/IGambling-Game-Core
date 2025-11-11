package com.nekzabirov.catalog.table

import com.nekzabirov.catalog.table.base.AbstractTable
import com.nekzabirov.catalog.value.ImageMap
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