package application.port.outbound

import domain.aggregator.model.AggregatorInfo
import domain.game.model.GameVariant

/**
 * Outbound port for game synchronization.
 * Implementation handles transaction management for complex sync operations.
 */
interface GameSyncAdapter {
    suspend fun syncGame(variants: List<GameVariant>, aggregatorInfo: AggregatorInfo)
}
