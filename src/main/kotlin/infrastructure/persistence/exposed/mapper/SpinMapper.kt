package infrastructure.persistence.exposed.mapper

import domain.session.model.Spin
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toSpin(): Spin = Spin(
    id = this[SpinTable.id].value,
    roundId = this[SpinTable.roundId]?.value ?: throw IllegalStateException("Spin must have a round"),
    type = this[SpinTable.type],
    amount = (this[SpinTable.amount] ?: 0L).toBigInteger(),
    realAmount = (this[SpinTable.realAmount] ?: 0L).toBigInteger(),
    bonusAmount = (this[SpinTable.bonusAmount] ?: 0L).toBigInteger(),
    extId = this[SpinTable.extId],
    referenceId = this[SpinTable.referenceId]?.value,
    freeSpinId = this[SpinTable.freeSpinId]
)
