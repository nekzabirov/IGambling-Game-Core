package domain.session.mapper

import core.value.Currency
import core.value.Locale
import domain.session.model.Session
import domain.session.table.SessionTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toSession() = Session(
    id = this[SessionTable.id].value,

    gameId = this[SessionTable.gameId].value,

    aggregatorId = this[SessionTable.aggregatorId].value,

    playerId = this[SessionTable.playerId],

    token = this[SessionTable.token],

    externalToken = this[SessionTable.external_token],

    currency = Currency(this[SessionTable.currency]),

    locale = Locale(this[SessionTable.locale]),

    platform = this[SessionTable.platform]
)