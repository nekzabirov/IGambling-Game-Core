package infrastructure.persistence.exposed.mapper

import domain.session.model.Round
import infrastructure.persistence.exposed.table.RoundTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toRound(): Round = Round(
    id = this[RoundTable.id].value,
    sessionId = this[RoundTable.sessionId].value,
    gameId = this[RoundTable.gameId].value,
    extId = this[RoundTable.extId],
    finished = this[RoundTable.finished],
    createdAt = this[RoundTable.createdAt],
    finishedAt = this[RoundTable.finishedAt]
)
