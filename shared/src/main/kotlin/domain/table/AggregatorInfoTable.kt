package domain.table

import domain.value.Aggregator
import domain.table.base.AbstractTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object AggregatorInfoTable : domain.table.base.AbstractTable("aggregators_info") {
    val identity = varchar("identity", 100)
        .uniqueIndex()

    val config = jsonb<Map<String, String>>("config", Json.Default)

    val aggregator = enumeration<domain.value.Aggregator>("aggregator")

    val active = bool("active")
        .default(false)


}