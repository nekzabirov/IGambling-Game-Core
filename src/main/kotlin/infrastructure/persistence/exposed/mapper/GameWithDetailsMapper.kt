package infrastructure.persistence.exposed.mapper

import domain.common.value.Locale
import domain.common.value.Platform
import domain.game.model.GameWithDetails
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameVariantTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toGameWithDetails(): GameWithDetails = GameWithDetails(
    id = this[GameTable.id].value,
    identity = this[GameTable.identity],
    name = this[GameTable.name],
    images = this[GameTable.images],
    bonusBetEnable = this[GameTable.bonusBetEnable],
    bonusWageringEnable = this[GameTable.bonusWageringEnable],
    tags = this[GameTable.tags],
    symbol = this[GameVariantTable.symbol],
    freeSpinEnable = this[GameVariantTable.freeSpinEnable],
    freeChipEnable = this[GameVariantTable.freeChipEnable],
    jackpotEnable = this[GameVariantTable.jackpotEnable],
    demoEnable = this[GameVariantTable.demoEnable],
    bonusBuyEnable = this[GameVariantTable.bonusBuyEnable],
    locales = this[GameVariantTable.locales].map { Locale(it) },
    platforms = this[GameVariantTable.platforms].map { Platform.valueOf(it) },
    playLines = this[GameVariantTable.playLines],
    provider = this.toProvider(),
    aggregator = this.toAggregatorInfo()
)
