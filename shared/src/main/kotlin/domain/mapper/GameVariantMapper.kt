package domain.mapper

import domain.value.Platform
import domain.model.GameVariant
import domain.table.GameVariantTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toGameVariant() = _root_ide_package_.domain.model.GameVariant(
    id = this[GameVariantTable.id].value,

    gameId = this[GameVariantTable.gameId]?.value,

    symbol = this[GameVariantTable.symbol],

    name = this[GameVariantTable.name],

    providerName = this[GameVariantTable.providerName],

    aggregator = this[GameVariantTable.aggregator],

    playLines = this[GameVariantTable.playLines],

    freeSpinEnable = this[GameVariantTable.freeSpinEnable],
    freeChipEnable = this[GameVariantTable.freeChipEnable],

    jackpotEnable = this[GameVariantTable.jackpotEnable],

    demoEnable = this[GameVariantTable.demoEnable],

    bonusBuyEnable = this[GameVariantTable.bonusBuyEnable],

    locales = this[GameVariantTable.locales].toList(),

    platforms = this[GameVariantTable.platforms].map {
        _root_ide_package_.domain.value.Platform.valueOf(
            it
        )
    },
)