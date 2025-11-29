package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.session.model.Spin
import com.nekgamebling.domain.session.repository.SpinRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toSpin
import com.nekgamebling.infrastructure.persistence.exposed.table.SpinTable
import com.nekgamebling.shared.value.SpinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of SpinRepository.
 */
class ExposedSpinRepository : BaseExposedRepository<Spin, SpinTable>(SpinTable), SpinRepository {

    override fun ResultRow.toEntity(): Spin = toSpin()

    override suspend fun findByRoundId(roundId: UUID): List<Spin> = findAllByNullableRef(SpinTable.roundId, roundId)

    override suspend fun findByRoundIdAndType(roundId: UUID, type: SpinType): Spin? = newSuspendedTransaction {
        table.selectAll()
            .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq type) }
            .singleOrNull()
            ?.toEntity()
    }

    override suspend fun findByExtId(extId: String): Spin? = findOneBy(SpinTable.extId, extId)

    override suspend fun save(spin: Spin): Spin = newSuspendedTransaction {
        val id = SpinTable.insertAndGetId {
            it[roundId] = spin.roundId
            it[type] = spin.type
            it[amount] = spin.amount
            it[realAmount] = spin.realAmount
            it[bonusAmount] = spin.bonusAmount
            it[extId] = spin.extId
            it[referenceId] = spin.referenceId
            it[freeSpinId] = spin.freeSpinId
        }
        spin.copy(id = id.value)
    }

    override suspend fun findPlaceSpinByRoundId(roundId: UUID): Spin? =
        findByRoundIdAndType(roundId, SpinType.PLACE)
}
