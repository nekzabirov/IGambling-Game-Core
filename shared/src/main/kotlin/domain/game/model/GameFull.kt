package domain.game.model

import core.value.Locale
import core.value.Platform
import domain.aggregator.model.AggregatorInfo
import domain.provider.model.Provider
import java.util.*

data class GameFull(
    val id: UUID,

    val identity: String,

    val name: String,

    val bonusBetEnable: Boolean = true,

    val bonusWageringEnable: Boolean = true,

    val tags: List<String> = emptyList(),

    val symbol: String,

    val freeSpinEnable: Boolean,

    val freeChipEnable: Boolean,

    val jackpotEnable: Boolean,

    val demoEnable: Boolean,

    val bonusBuyEnable: Boolean,

    val locales: List<Locale>,

    val platforms: List<Platform>,

    val playLines: Int,

    val provider: Provider,

    val aggregator: AggregatorInfo,
)
