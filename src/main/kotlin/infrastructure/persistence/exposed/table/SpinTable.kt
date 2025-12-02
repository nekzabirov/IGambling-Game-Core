package infrastructure.persistence.exposed.table

import shared.value.SpinType

object SpinTable : BaseTable("spins") {
    val roundId = reference("round_id", RoundTable.id).nullable()
    val type = enumeration<SpinType>("type")
    val amount = long("amount").nullable()
    val realAmount = long("real_amount").nullable()
    val bonusAmount = long("bonus_amount").nullable()
    val extId = varchar("ext_id", 255)
    val referenceId = reference("reference_id", SpinTable.id).nullable()
    val freeSpinId = varchar("free_spin_id", 255).nullable()
}
