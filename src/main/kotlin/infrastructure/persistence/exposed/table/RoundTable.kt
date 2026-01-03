package infrastructure.persistence.exposed.table

import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object RoundTable : BaseTable("rounds") {
    val sessionId = reference("session_id", SessionTable.id)
    val gameId = reference("game_id", GameTable.id)
    val extId = varchar("ext_id", 255)
    val finished = bool("finished").default(false)
    val finishedAt = datetime("finished_at").nullable()

    init {
        uniqueIndex(sessionId, extId)
    }
}
