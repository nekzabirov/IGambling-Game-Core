package domain.game.table

import core.db.AbstractTable

object GameWonTable : AbstractTable("game_wons") {
    val gameId = reference("game_id", GameTable.id)

    val playerId = varchar("player_id", 100)

    val amount = integer("amount")

    val currency = varchar("currency", 3)
}