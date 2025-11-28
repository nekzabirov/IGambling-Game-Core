package com.nekgamebling.domain.game.model

import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform
import java.util.UUID

/**
 * Game variant representing an aggregator-specific version of a game.
 */
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
    val locales: List<Locale>,
    val platforms: List<Platform>,
    val playLines: Int = 0
) {
    init {
        require(symbol.isNotBlank()) { "Game variant symbol cannot be blank" }
        require(name.isNotBlank()) { "Game variant name cannot be blank" }
    }

    fun isLinkedToGame(): Boolean = gameId != null
    fun supportsPlatform(platform: Platform): Boolean = platforms.contains(platform)
    fun supportsLocale(locale: Locale): Boolean = locales.contains(locale)
}
