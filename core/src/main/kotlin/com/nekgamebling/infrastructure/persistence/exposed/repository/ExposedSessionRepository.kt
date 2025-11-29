package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.session.model.Session
import com.nekgamebling.domain.session.repository.SessionRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toSession
import com.nekgamebling.infrastructure.persistence.exposed.table.SessionTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of SessionRepository.
 */
class ExposedSessionRepository : BaseExposedRepository<Session, SessionTable>(SessionTable), SessionRepository {

    override fun ResultRow.toEntity(): Session = toSession()

    override suspend fun findByToken(token: String): Session? = findOneBy(SessionTable.token, token)

    override suspend fun save(session: Session): Session = newSuspendedTransaction {
        val id = SessionTable.insertAndGetId {
            it[gameId] = session.gameId
            it[aggregatorId] = session.aggregatorId
            it[playerId] = session.playerId
            it[token] = session.token
            it[externalToken] = session.externalToken
            it[currency] = session.currency.value
            it[locale] = session.locale.value
            it[platform] = session.platform
        }
        session.copy(id = id.value)
    }
}
