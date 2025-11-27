package domain.game.table

import org.jetbrains.exposed.sql.Table

object GameFavouriteTable : Table("games_favourites") {
    val gameId = reference("game_id", GameTable.id)

    val playerId = varchar("player_id", 100)

    override val primaryKey: PrimaryKey = PrimaryKey(gameId, playerId)
}