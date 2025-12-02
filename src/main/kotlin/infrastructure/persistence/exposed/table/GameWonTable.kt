package infrastructure.persistence.exposed.table

object GameWonTable : BaseTable("game_wons") {
    val gameId = reference("game_id", GameTable.id)
    val playerId = varchar("player_id", 100)
    val amount = long("amount")
    val currency = varchar("currency", 3)
}
