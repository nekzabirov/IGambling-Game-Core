package infrastructure.persistence.exposed.mapper

import domain.game.model.Game
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toGame(): Game = Game(
    id = this[GameTable.id].value,
    identity = this[GameTable.identity],
    name = this[GameTable.name],
    providerId = this[GameTable.providerId].value,
    images = this[GameTable.images],
    bonusBetEnable = this[GameTable.bonusBetEnable],
    bonusWageringEnable = this[GameTable.bonusWageringEnable],
    tags = this[GameTable.tags],
    active = this[GameTable.active]
)
