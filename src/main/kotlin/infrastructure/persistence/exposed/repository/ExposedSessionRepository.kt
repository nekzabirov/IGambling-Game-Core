package infrastructure.persistence.exposed.repository

import domain.session.model.Session
import domain.session.repository.SessionRepository
import infrastructure.persistence.exposed.mapper.toSession
import infrastructure.persistence.exposed.table.SessionTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of SessionRepository.
 */
class ExposedSessionRepository : SessionRepository {

    override suspend fun findById(id: UUID): Session? = newSuspendedTransaction {
        SessionTable.selectAll()
            .where { SessionTable.id eq id }
            .singleOrNull()
            ?.toSession()
    }

    override suspend fun findByToken(token: String): Session? = newSuspendedTransaction {
        SessionTable.selectAll()
            .where { SessionTable.token eq token }
            .singleOrNull()
            ?.toSession()
    }

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

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        SessionTable.deleteWhere { SessionTable.id eq id } > 0
    }
}
