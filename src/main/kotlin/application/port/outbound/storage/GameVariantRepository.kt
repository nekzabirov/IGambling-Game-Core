package application.port.outbound

import domain.game.model.GameVariant

/**
 * Port interface for GameVariant entity persistence operations.
 */
interface GameVariantRepository {
    /**
     * Save all variants (upsert operation).
     * Returns variants with generated IDs.
     */
    suspend fun saveAll(variants: List<GameVariant>): List<GameVariant>
}
