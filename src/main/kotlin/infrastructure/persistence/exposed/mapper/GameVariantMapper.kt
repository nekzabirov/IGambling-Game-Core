package infrastructure.persistence.exposed.mapper

import domain.common.value.Locale
import domain.common.value.Platform
import domain.game.model.GameVariant
import infrastructure.persistence.exposed.table.GameVariantTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toGameVariant(): GameVariant = GameVariant(
    id = this[GameVariantTable.id].value,
    gameId = this[GameVariantTable.gameId]?.value,
    symbol = this[GameVariantTable.symbol],
    name = this[GameVariantTable.name],
    providerName = this[GameVariantTable.providerName],
    aggregator = this[GameVariantTable.aggregator],
    freeSpinEnable = this[GameVariantTable.freeSpinEnable],
    freeChipEnable = this[GameVariantTable.freeChipEnable],
    jackpotEnable = this[GameVariantTable.jackpotEnable],
    demoEnable = this[GameVariantTable.demoEnable],
    bonusBuyEnable = this[GameVariantTable.bonusBuyEnable],
    locales = this[GameVariantTable.locales].map { Locale(it) },
    platforms = this[GameVariantTable.platforms].map { Platform.valueOf(it) },
    playLines = this[GameVariantTable.playLines]
)
