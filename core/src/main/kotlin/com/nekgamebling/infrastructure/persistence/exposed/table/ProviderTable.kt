package com.nekgamebling.infrastructure.persistence.exposed.table

import com.nekgamebling.infrastructure.persistence.exposed.repository.IdentityTable
import com.nekgamebling.shared.value.ImageMap
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object ProviderTable : BaseTable("providers"), IdentityTable {
    override val identity = varchar("identity", 100).uniqueIndex()
    val name = varchar("name", 100)
    val images = jsonb<ImageMap>("images", Json.Default).default(ImageMap.EMPTY)
    val order = integer("order").default(100)
    val aggregatorId = reference("aggregator_id", AggregatorInfoTable.id).nullable()
    val active = bool("active").default(true)
}
