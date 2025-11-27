package domain.game.mapper

import core.value.Locale
import core.model.Platform
import domain.aggregator.mapper.toAggregatorModel
import domain.game.model.Game
import domain.game.model.GameFull
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.mapper.toProvider
import org.jetbrains.exposed.sql.ResultRow
import kotlin.collections.map
import kotlin.collections.toList

fun ResultRow.toGame() = Game(
    id = this[GameTable.id].value,

    identity = this[GameTable.identity],

    name = this[GameTable.name],

    images = this[GameTable.images],

    providerId = this[GameTable.providerId].value,

    bonusBetEnable = this[GameTable.bonusBetEnable],

    bonusWageringEnable = this[GameTable.bonusWageringEnable],

    tags = this[GameTable.tags].toList(),

    active = this[GameTable.active],
)

fun ResultRow.toGameFull() = GameFull(
    id = this[GameTable.id].value,

    identity = this[GameTable.identity],

    name = this[GameTable.name],

    images = this[GameTable.images],

    bonusBetEnable = this[GameTable.bonusBetEnable],

    bonusWageringEnable = this[GameTable.bonusWageringEnable],

    tags = this[GameTable.tags].toList(),

    symbol = this[GameVariantTable.symbol],

    freeSpinEnable = this[GameVariantTable.freeSpinEnable],

    freeChipEnable = this[GameVariantTable.freeChipEnable],

    jackpotEnable = this[GameVariantTable.jackpotEnable],

    demoEnable = this[GameVariantTable.demoEnable],

    bonusBuyEnable = this[GameVariantTable.bonusBuyEnable],

    locales = this[GameVariantTable.locales].toList().map { Locale(it) },

    platforms = this[GameVariantTable.platforms].map { Platform.valueOf(it) },

    playLines = this[GameVariantTable.playLines],

    provider = this.toProvider(),

    aggregator = this.toAggregatorModel(),
)