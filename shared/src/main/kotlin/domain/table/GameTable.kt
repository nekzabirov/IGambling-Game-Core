package domain.table

import domain.table.base.AbstractTable
import domain.value.ImageMap
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object GameTable : domain.table.base.AbstractTable("games") {
    val identity = varchar("identity", 100)
        .uniqueIndex()

    val name = varchar("name", 100)

    val providerId = reference("provider_id", ProviderTable.id)

    val images = jsonb<domain.value.ImageMap>("images", Json.Default)
        .default(_root_ide_package_.domain.value.ImageMap(emptyMap()))

    val bonusBetEnable = bool("bonus_bet_enable")
        .default(true)

    val bonusWageringEnable = bool("bonus_wagering_enable")
        .default(true)

    val tags = array<String>("tags")
        .default(emptyList())

    val active = bool("active")
        .default(true)
}