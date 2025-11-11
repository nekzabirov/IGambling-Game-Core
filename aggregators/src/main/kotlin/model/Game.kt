package com.nekzabirov.aggregators.model

import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.aggregators.value.Locale
import com.nekzabirov.aggregators.value.Platform

data class Game(
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
