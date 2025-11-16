package domain.session.table

import core.db.AbstractTable
import core.model.Platform
import domain.aggregator.table.AggregatorInfoTable
import domain.game.table.GameTable

object SessionTable : AbstractTable("sessions") {
    val gameId = reference("game_id", GameTable.id)

    val aggregatorId = reference("aggregator_id", AggregatorInfoTable.id)

    val playerId = varchar("player_id", 100)

    val token = varchar("token", 255)

    val external_token = varchar("external_token", 255)
        .nullable()
        .default(null)

    val currency = varchar("currency", 3)

    val locale = varchar("locale", 2)

    val platform = enumeration<Platform>("platform")
}