package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.session.model.Round
import com.nekgamebling.domain.session.repository.RoundRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toRound
import com.nekgamebling.infrastructure.persistence.exposed.table.RoundTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Exposed implementation of RoundRepository.
 */
class ExposedRoundRepository : BaseExposedRepository<Round, RoundTable>(RoundTable), RoundRepository {

    override fun ResultRow.toEntity(): Round = toRound()

    override suspend fun findByExtId(sessionId: UUID, extId: String): Round? = newSuspendedTransaction {
        table.selectAll()
            .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
            .singleOrNull()
            ?.toEntity()
    }

    override suspend fun findBySessionId(sessionId: UUID): List<Round> = findAllByRef(RoundTable.sessionId, sessionId)

    override suspend fun save(round: Round): Round = newSuspendedTransaction {
        val id = RoundTable.insertAndGetId {
            it[sessionId] = round.sessionId
            it[gameId] = round.gameId
            it[extId] = round.extId
            it[finished] = round.finished
        }
        round.copy(id = id.value)
    }

    override suspend fun update(round: Round): Round = newSuspendedTransaction {
        RoundTable.update({ RoundTable.id eq round.id }) {
            it[finished] = round.finished
        }
        round
    }

    override suspend fun finish(id: UUID): Boolean = newSuspendedTransaction {
        RoundTable.update({ RoundTable.id eq id }) {
            it[finished] = true
        } > 0
    }

    override suspend fun findOrCreate(sessionId: UUID, gameId: UUID, extId: String): Round = newSuspendedTransaction {
        val existing = table.selectAll()
            .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
            .singleOrNull()
            ?.toEntity()

        if (existing != null) return@newSuspendedTransaction existing

        val id = RoundTable.insertAndGetId {
            it[RoundTable.sessionId] = sessionId
            it[RoundTable.gameId] = gameId
            it[RoundTable.extId] = extId
            it[finished] = false
        }

        Round(
            id = id.value,
            sessionId = sessionId,
            gameId = gameId,
            extId = extId,
            finished = false
        )
    }
}
