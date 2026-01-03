package infrastructure.external.turbo.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponseDto(
    val balance: Long,
    val realBalance: Long,
    val bonusBalance: Long,
    val lockedBalance: Long = 0,
    val currency: String
)
