package domain.collection.table

import domain.game.table.GameTable
import org.jetbrains.exposed.sql.Table

object CollectionGameTable : Table("collection_games") {
    val categoryId = reference("category_id", CollectionTable.id)

    val gameId = reference("game_id", GameTable.id)

    val order = integer("order").default(100)

    override val primaryKey: PrimaryKey = PrimaryKey(categoryId, gameId)
}