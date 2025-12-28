package domain.game.repository

import domain.common.value.Aggregator

/**
 * Filter criteria for game variant queries.
 */
data class GameVariantFilter(
    val query: String = "",
    val aggregator: Aggregator? = null,
    val gameIdentity: String? = null
) {
    class Builder {
        private var query: String = ""
        private var aggregator: Aggregator? = null
        private var gameIdentity: String? = null

        fun withQuery(query: String) = apply { this.query = query }
        fun withAggregator(aggregator: Aggregator?) = apply { this.aggregator = aggregator }
        fun withGameIdentity(gameIdentity: String?) = apply { this.gameIdentity = gameIdentity }

        fun build() = GameVariantFilter(query, aggregator, gameIdentity)
    }

    companion object {
        fun builder() = Builder()
        val EMPTY = GameVariantFilter()
    }
}
