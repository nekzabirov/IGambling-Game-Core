package com.nekzabirov.catalog.model

import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.aggregators.value.Platform
import java.util.UUID

data class GameVariant(
    val id: UUID = UUID.randomUUID(),

    val gameId: UUID? = null,

    val symbol: String,

    val name: String,

    val providerName: String,

    val aggregator: Aggregator,

    val freeSpinEnable: Boolean,

    val freeChipEnable: Boolean,

    val jackpotEnable: Boolean,

    val demoEnable: Boolean,

    val bonusBuyEnable: Boolean,

    val locales: List<String>,

    val platforms: List<Platform>,

    val playLines: Int = 0,
)
