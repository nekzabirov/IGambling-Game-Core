package com.nekgamebling.infrastructure.persistence.exposed.table

import com.nekgamebling.infrastructure.persistence.exposed.repository.IdentityTable
import com.nekgamebling.shared.value.Aggregator
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object AggregatorInfoTable : BaseTable("aggregator_infos"), IdentityTable {
    override val identity = varchar("identity", 100).uniqueIndex()
    val config = jsonb<Map<String, String>>("config", Json.Default)
    val aggregator = enumeration<Aggregator>("aggregator")
    val active = bool("active").default(true)
}
