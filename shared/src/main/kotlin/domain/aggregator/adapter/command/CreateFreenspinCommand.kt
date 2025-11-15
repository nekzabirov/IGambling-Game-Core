package domain.aggregator.adapter.command

import core.value.Currency
import kotlinx.datetime.LocalDateTime

data class CreateFreenspinCommand(
    val presetValue: Map<String, Int>,

    val referenceId: String,

    val playerId: String,

    val gameSymbol: String,

    val currency: Currency,

    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)