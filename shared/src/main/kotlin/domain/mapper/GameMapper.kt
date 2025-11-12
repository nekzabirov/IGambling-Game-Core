package domain.mapper

import domain.model.Game
import domain.table.GameTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toGame() = _root_ide_package_.domain.model.Game(
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