package com.nekgamebling.infrastructure.turbo.dto

import kotlinx.serialization.Serializable

@Serializable
data class SettleTransactionRequest(
    val playerId: String,
    val amount: Long,
    val currency: String,
    val externalId: String,
    val referencedExternalId: String,
    val balanceType: BalanceType
)
