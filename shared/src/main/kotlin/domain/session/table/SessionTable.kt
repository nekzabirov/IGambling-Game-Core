package domain.session.table

import core.db.AbstractTable
import core.value.Platform
import domain.game.table.GameTable

object SessionTable : AbstractTable("sessions") {
    val gameId = reference("game_id", GameTable.id)

    val playerId = varchar("player_id", 100)

    val token = varchar("token", 255)

    val external_token = varchar("external_token", 255)
        .nullable()
        .default(null)

    val currency = varchar("currency", 3)

    val locale = varchar("locale", 2)

    val platform = enumeration<Platform>("platform")
}