package com.nekgamebling.domain.aggregator.model

import com.nekgamebling.shared.serializer.UUIDSerializer
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Aggregator configuration entity.
 */
@Serializable
data class AggregatorInfo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val identity: String,
    val config: Map<String, String>,
    val aggregator: Aggregator,
    val active: Boolean = true
) {
    init {
        require(identity.isNotBlank()) { "Aggregator identity cannot be blank" }
    }

    fun getConfigValue(key: String): String? = config[key]
    fun requireConfigValue(key: String): String =
        config[key] ?: throw IllegalStateException("Missing required config key: $key")

    fun activate(): AggregatorInfo = copy(active = true)
    fun deactivate(): AggregatorInfo = copy(active = false)
}

/**
 * Game information from an aggregator.
 */
data class AggregatorGame(
    val symbol: String,
    val name: String,
    val providerName: String,
    val freeSpinEnable: Boolean,
    val freeChipEnable: Boolean,
    val jackpotEnable: Boolean,
    val demoEnable: Boolean,
    val bonusBuyEnable: Boolean,
    val locales: List<Locale>,
    val platforms: List<Platform>,
    val playLines: Int = 0
)
