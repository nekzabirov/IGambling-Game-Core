package infrastructure.aggregator.pragmatic.handler.dto

data class PragmaticBetPayload(
    val reference: String,

    val gameId: String,

    val bonusCode: String?,

    val roundId: String,

    val amount: String,

    val promoWinAmount: String = "0"
)
