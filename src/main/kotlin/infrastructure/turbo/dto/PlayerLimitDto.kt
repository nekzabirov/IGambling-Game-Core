package com.nekgamebling.infrastructure.turbo.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerLimitDto(
    val id: String,
    val playerId: String,
    val definition: Int,
    val status: Int,
    val amount: Long,
    val currentAmount: Long?,
    val currency: String,
    val period: String,
    val startedAt: String,
    val expiredAt: String?,
    val createdAt: String,
    val updatedAt: String
) {
    fun isActive() = status == 1

    fun isPlaceBet() = definition == 1

    fun getRestAmount(): Int {
        if (currentAmount == null) return amount.toInt()
        return (amount - currentAmount).toInt()
    }
}
