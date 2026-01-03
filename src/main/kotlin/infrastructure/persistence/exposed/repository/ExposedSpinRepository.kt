package infrastructure.persistence.exposed.repository

import application.port.outbound.SpinRepository
import domain.common.value.SpinType
import domain.session.model.Spin
import infrastructure.persistence.exposed.mapper.toSpin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of SpinRepository.
 */
class ExposedSpinRepository : SpinRepository {

    override suspend fun findByRoundAndType(roundId: UUID, type: SpinType): Spin? =
        newSuspendedTransaction {
            SpinTable.selectAll()
                .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq type) }
                .singleOrNull()
                ?.toSpin()
        }

    override suspend fun findAllByRound(roundId: UUID): List<Spin> =
        newSuspendedTransaction {
            SpinTable.selectAll()
                .where { SpinTable.roundId eq roundId }
                .map { it.toSpin() }
        }

    override suspend fun save(spin: Spin): Spin =
        newSuspendedTransaction {
            val id = SpinTable.insertAndGetId {
                it[roundId] = spin.roundId
                it[type] = spin.type
                it[amount] = spin.amount.toLong()
                it[realAmount] = spin.realAmount.toLong()
                it[bonusAmount] = spin.bonusAmount.toLong()
                it[extId] = spin.extId
                it[referenceId] = spin.referenceId
                it[freeSpinId] = spin.freeSpinId
            }
            spin.copy(id = id.value)
        }
}
