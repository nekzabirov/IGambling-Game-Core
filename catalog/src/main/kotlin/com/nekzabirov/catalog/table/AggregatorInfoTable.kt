package com.nekzabirov.catalog.table

import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.catalog.table.base.AbstractTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object AggregatorInfoTable : AbstractTable("aggregators_info") {
    val identity = varchar("identity", 100)
        .uniqueIndex()

    val config = jsonb<Map<String, String>>("config", Json.Default)

    val aggregator = enumeration<Aggregator>("aggregator")

    val active = bool("active")
        .default(false)


}