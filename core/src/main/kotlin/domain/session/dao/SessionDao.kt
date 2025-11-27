package domain.session.dao

import domain.session.mapper.toSession
import domain.session.table.SessionTable
import org.jetbrains.exposed.sql.selectAll

fun SessionTable.findByToken(token: String) = selectAll()
    .where { SessionTable.token eq token }
    .singleOrNull()?.toSession()