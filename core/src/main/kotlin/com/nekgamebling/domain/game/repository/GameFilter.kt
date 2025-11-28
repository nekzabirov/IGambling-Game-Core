package com.nekgamebling.domain.game.repository

import com.nekgamebling.shared.value.Platform

/**
 * Filter criteria for game queries.
 */
data class GameFilter(
    val query: String = "",
    val active: Boolean? = null,
    val bonusBet: Boolean? = null,
    val bonusWagering: Boolean? = null,
    val freeSpinEnable: Boolean? = null,
    val freeChipEnable: Boolean? = null,
    val jackpotEnable: Boolean? = null,
    val demoEnable: Boolean? = null,
    val bonusBuyEnable: Boolean? = null,
    val platforms: List<Platform> = emptyList(),
    val providerIdentities: List<String> = emptyList(),
    val collectionIdentities: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val playerId: String? = null
) {
    class Builder {
        private var query: String = ""
        private var active: Boolean? = null
        private var bonusBet: Boolean? = null
        private var bonusWagering: Boolean? = null
        private var freeSpinEnable: Boolean? = null
        private var freeChipEnable: Boolean? = null
        private var jackpotEnable: Boolean? = null
        private var demoEnable: Boolean? = null
        private var bonusBuyEnable: Boolean? = null
        private val platforms = mutableListOf<Platform>()
        private val providerIdentities = mutableListOf<String>()
        private val collectionIdentities = mutableListOf<String>()
        private val tags = mutableListOf<String>()
        private var playerId: String? = null

        fun query(query: String) = apply { this.query = query }
        fun active(active: Boolean?) = apply { this.active = active }
        fun bonusBet(bonusBet: Boolean?) = apply { this.bonusBet = bonusBet }
        fun bonusWagering(bonusWagering: Boolean?) = apply { this.bonusWagering = bonusWagering }
        fun freeSpinEnable(freeSpinEnable: Boolean?) = apply { this.freeSpinEnable = freeSpinEnable }
        fun freeChipEnable(freeChipEnable: Boolean?) = apply { this.freeChipEnable = freeChipEnable }
        fun jackpotEnable(jackpotEnable: Boolean?) = apply { this.jackpotEnable = jackpotEnable }
        fun demoEnable(demoEnable: Boolean?) = apply { this.demoEnable = demoEnable }
        fun bonusBuyEnable(bonusBuyEnable: Boolean?) = apply { this.bonusBuyEnable = bonusBuyEnable }
        fun platform(platform: Platform) = apply { platforms.add(platform) }
        fun platforms(platforms: List<Platform>) = apply { this.platforms.addAll(platforms) }
        fun providerIdentity(identity: String) = apply { providerIdentities.add(identity) }
        fun collectionIdentity(identity: String) = apply { collectionIdentities.add(identity) }
        fun tag(tag: String) = apply { tags.add(tag) }
        fun playerId(playerId: String?) = apply { this.playerId = playerId }

        fun build() = GameFilter(
            query = query,
            active = active,
            bonusBet = bonusBet,
            bonusWagering = bonusWagering,
            freeSpinEnable = freeSpinEnable,
            freeChipEnable = freeChipEnable,
            jackpotEnable = jackpotEnable,
            demoEnable = demoEnable,
            bonusBuyEnable = bonusBuyEnable,
            platforms = platforms.toList(),
            providerIdentities = providerIdentities.toList(),
            collectionIdentities = collectionIdentities.toList(),
            tags = tags.toList(),
            playerId = playerId
        )
    }

    companion object {
        fun builder() = Builder()
        val EMPTY = GameFilter()
    }
}
