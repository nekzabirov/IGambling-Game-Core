package domain.aggregator.model

import core.value.Locale
import core.model.Platform

data class AggregatorGame(
    val symbol: String,

    val name: String,

    val providerName: String,

    val aggregator: Aggregator,

    val freeSpinEnable: Boolean,

    val freeChipEnable: Boolean,

    val jackpotEnable: Boolean,

    val demoEnable: Boolean,

    val bonusBuyEnable: Boolean,

    val locales: List<Locale>,

    val playLines: Int = 0,

    val platforms: List<Platform>,
)