package com.nekgamebling.application.port.inbound.spin

import application.port.inbound.Query
import domain.session.model.Round
import shared.value.Currency

data class FindRoundQuery(
    val id: String
) : Query<FindRoundQueryResult>

data class FindRoundQueryResult(
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
