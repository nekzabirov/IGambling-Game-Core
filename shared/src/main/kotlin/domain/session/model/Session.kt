package domain.session.model

import core.value.Currency
import core.value.Locale
import core.value.Platform
import domain.game.table.GameTable
import java.util.UUID

data class Session(
    val id: UUID = UUID.randomUUID(),

    val gameId: UUID,

    val playerId: String,

    val token: String,

    val externalToken: String?,

    val currency: Currency,

    val locale: Locale,

    val platform: Platform
)
