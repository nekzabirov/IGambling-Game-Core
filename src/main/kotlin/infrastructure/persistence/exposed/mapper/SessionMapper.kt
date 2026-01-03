package infrastructure.persistence.exposed.mapper

import domain.common.value.Locale
import domain.session.model.Session
import infrastructure.persistence.exposed.table.SessionTable
import org.jetbrains.exposed.sql.ResultRow
import shared.value.Currency

fun ResultRow.toSession(): Session = Session(
    id = this[SessionTable.id].value,
    gameId = this[SessionTable.gameId].value,
    aggregatorId = this[SessionTable.aggregatorId].value,
    playerId = this[SessionTable.playerId],
    token = this[SessionTable.token],
    externalToken = this[SessionTable.externalToken],
    currency = Currency(this[SessionTable.currency]),
    locale = Locale(this[SessionTable.locale]),
    platform = this[SessionTable.platform]
)
