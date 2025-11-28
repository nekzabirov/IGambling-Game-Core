package com.nekgamebling.domain.game.repository

import com.nekgamebling.domain.collection.model.Collection
import com.nekgamebling.domain.game.model.Game
import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.domain.provider.model.Provider

/**
 * Game list item containing game with variant, provider and collections.
 */
data class GameListItem(
    val game: Game,
    val variant: GameVariant,
    val provider: Provider,
    val collections: List<Collection> = emptyList()
)
