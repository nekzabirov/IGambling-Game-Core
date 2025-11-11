package com.nekzabirov.aggregators.command

import com.nekzabirov.aggregators.core.IAggregatorPreset
import com.nekzabirov.aggregators.value.Currency
import kotlinx.datetime.LocalDateTime

data class CreateFreenspinCommand(
    val preset: IAggregatorPreset,

    val referenceId: String,

    val playerId: String,

    val gameSymbol: String,

    val currency: Currency,

    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)