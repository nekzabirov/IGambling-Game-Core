package domain.session.repository

/**
 * Filter criteria for round queries.
 */
data class RoundFilter(
    val playerId: String? = null,
    val gameIdentity: String? = null
) {
    class Builder {
        private var playerId: String? = null
        private var gameIdentity: String? = null

        fun playerId(playerId: String?) = apply { this.playerId = playerId }
        fun gameIdentity(gameIdentity: String?) = apply { this.gameIdentity = gameIdentity }

        fun build() = RoundFilter(
            playerId = playerId,
            gameIdentity = gameIdentity
        )
    }

    companion object {
        fun builder() = Builder()
        val EMPTY = RoundFilter()
    }
}
