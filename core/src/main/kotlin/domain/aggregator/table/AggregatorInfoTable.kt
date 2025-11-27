package domain.aggregator.table

import core.db.AbstractTable
import domain.aggregator.model.Aggregator
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