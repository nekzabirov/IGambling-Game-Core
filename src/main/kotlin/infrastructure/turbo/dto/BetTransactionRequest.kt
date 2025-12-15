package com.nekgamebling.infrastructure.turbo.dto

import kotlinx.serialization.Serializable

@Serializable
data class BetTransactionRequest(
    val playerId: String,
    val amount: Long,
    val currency: String,
    val externalId: String,
    val balanceTypeOrder: List<BalanceType>,
    val maxBetAmount: Long? = null
)
