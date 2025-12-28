package domain.game.repository

import domain.collection.model.Collection
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.provider.model.Provider

/**
 * Game list item containing game with variant, provider and collections.
 *
 * Uses MutableSet for collections to enable O(1) contains checks
 * and avoid duplicates from JOIN results.
 */
data class GameListItem(
    val game: Game,
    val variant: GameVariant,
    val provider: Provider,
    val collections: MutableSet<Collection> = linkedSetOf()
) {
    /**
     * Get collections as immutable list.
     */
    fun collectionsList(): List<Collection> = collections.toList()
}
