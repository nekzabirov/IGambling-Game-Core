package domain.session.table

import core.db.AbstractTable
import domain.game.table.GameTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object RoundTable : AbstractTable("rounds") {
    val sessionId = reference("session_id", SessionTable.id)

    val gameId = reference("game_id", GameTable.id)

    val extId = varchar("ext_id", 255)

    val endAt = datetime("end_at")
        .nullable()
        .default(null)

    val freespinId = varchar("freespin_id", 255)
        .nullable()
        .default(null)

    init {
        uniqueIndex(sessionId, extId)
    }
}