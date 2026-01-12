package infrastructure.aggregator.onegamehub.handler.dto

data class OneGameHubBetDto(
    val gameSymbol: String,

    val roundId: String,

    val transactionId: String,

    val freeSpinId: String?,

    val amount: Long,

    val finishRound: Boolean = false
)
