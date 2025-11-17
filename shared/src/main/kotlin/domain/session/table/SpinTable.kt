package domain.session.table

import core.db.AbstractTable
import core.model.SpinType

object SpinTable : AbstractTable("spins") {
    val roundId = reference("round_id", RoundTable.id)

    val referenceId = reference("reference_id", SpinTable.id)
        .nullable()
        .default(null)

    val extId = varchar("ext_id", 255)

    val type = enumeration<SpinType>("type")

    val amount = integer("amount")

    val realAmount = integer("real_amount")

    val bonusAmount = integer("bonus_amount")
}