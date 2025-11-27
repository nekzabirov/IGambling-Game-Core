package domain.game.table

import domain.provider.table.ProviderTable
import core.db.AbstractTable
import core.value.ImageMap
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object GameTable : AbstractTable("games") {
    val identity = varchar("identity", 100)
        .uniqueIndex()

    val name = varchar("name", 100)

    val providerId = reference("provider_id", ProviderTable.id)

    val images = jsonb<ImageMap>("images", Json.Default)
        .default(ImageMap(emptyMap()))

    val bonusBetEnable = bool("bonus_bet_enable")
        .default(true)

    val bonusWageringEnable = bool("bonus_wagering_enable")
        .default(true)

    val tags = array<String>("tags")
        .default(emptyList())

    val active = bool("active")
        .default(true)
}