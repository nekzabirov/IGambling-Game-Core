package com.nekzabirov.catalog.mapper

import com.nekzabirov.catalog.model.Game
import com.nekzabirov.catalog.table.GameTable
import org.jetbrains.exposed.sql.ResultRow

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