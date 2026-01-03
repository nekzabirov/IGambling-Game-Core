package infrastructure.persistence.exposed.repository

import application.port.outbound.RoundRepository
import domain.common.value.SpinType
import domain.session.model.Round
import domain.session.model.Spin
import infrastructure.persistence.exposed.mapper.toRound
import infrastructure.persistence.exposed.mapper.toSpin
import infrastructure.persistence.exposed.table.RoundTable
import infrastructure.persistence.exposed.table.SpinTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning
import java.util.UUID

/**
 * Exposed implementation of RoundRepository.
 */
class ExposedRoundRepository : RoundRepository {

    override suspend fun findBySessionAndExtId(sessionId: UUID, extId: String): Round? =
        newSuspendedTransaction {
            RoundTable.selectAll()
                .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
                .singleOrNull()
                ?.toRound()
        }

    override suspend fun findWithPlaceSpin(sessionId: UUID, extId: String): Pair<Round, Spin>? =
        newSuspendedTransaction {
            (RoundTable innerJoin SpinTable)
                .selectAll()
                .where {
                    (RoundTable.sessionId eq sessionId) and
                    (RoundTable.extId eq extId) and
                    (SpinTable.type eq SpinType.PLACE)
                }
                .singleOrNull()
                ?.let { row ->
                    row.toRound() to row.toSpin()
                }
        }

    override suspend fun findOrCreate(sessionId: UUID, gameId: UUID, extId: String): Round =
        newSuspendedTransaction {
            val existing = RoundTable.selectAll()
                .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
                .singleOrNull()
                ?.toRound()

            existing ?: run {
                val row = RoundTable.upsertReturning(
                    keys = arrayOf(RoundTable.sessionId, RoundTable.extId),
                    onUpdateExclude = listOf(RoundTable.id, RoundTable.gameId, RoundTable.finished)
                ) {
                    it[RoundTable.sessionId] = sessionId
                    it[RoundTable.gameId] = gameId
                    it[RoundTable.extId] = extId
                    it[finished] = false
                }.single()
                row.toRound()
            }
        }

    override suspend fun finish(roundId: UUID): Unit =
        newSuspendedTransaction {
            RoundTable.update({ RoundTable.id eq roundId }) {
                it[finished] = true
                it[finishedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }
}
