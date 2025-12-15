package com.nekgamebling.infrastructure.turbo.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val realBalance: Long,
    val bonusBalance: Long,
    val lockedBalance: Long,
    val currency: String,
    val status: Int
)
