package com.nekgamebling.application.port.inbound.spin

import application.port.inbound.Query
import domain.game.model.Game
import domain.provider.model.Provider
import domain.session.model.Round
import shared.value.Currency
import kotlinx.datetime.LocalDateTime
import shared.value.Page
import shared.value.Pageable

data class FindAllRoundQuery(
    val pageable: Pageable,

    val gameIdentity: String?,
    val providerIdentity: String?,
    val finished: Boolean?,
    val playerId: String?,
    val freeSpinId: String?,

    val startAt: LocalDateTime? = null,
    val endAt: LocalDateTime? = null
) : Query<FindAllRoundQueryResult>

data class FindAllRoundQueryResult(
    val items: Page<RoundItem>,
    val providers: List<Provider>,
    val games: List<Game>
)

data class RoundItem(
    val round: Round,

    val providerIdentity: String,
    val gameIdentity: String,

    val playerId: String,
    val currency: Currency,

    val totalPlaceReal: Long,
    val totalPlaceBonus: Long,
    val totalSettleReal: Long,
    val totalSettleBonus: Long
)
