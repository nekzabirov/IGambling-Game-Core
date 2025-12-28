package infrastructure.persistence.exposed.table

import domain.common.value.Platform

object SessionTable : BaseTable("sessions") {
    val gameId = reference("game_id", GameTable.id)
    val aggregatorId = reference("aggregator_id", AggregatorInfoTable.id)
    val playerId = varchar("player_id", 100)
    val token = varchar("token", 255)
    val externalToken = varchar("external_token", 255).nullable().default(null)
    val currency = varchar("currency", 3)
    val locale = varchar("locale", 10)
    val platform = enumeration<Platform>("platform")
}
