package infrastructure.aggregator.onegamehub.handler.dto

import java.math.BigInteger

data class OneGameHubBetDto(
    val gameSymbol: String,

    val roundId: String,

    val transactionId: String,

    val freeSpinId: String?,

    val amount: BigInteger,

    val finishRound: Boolean = false
)
