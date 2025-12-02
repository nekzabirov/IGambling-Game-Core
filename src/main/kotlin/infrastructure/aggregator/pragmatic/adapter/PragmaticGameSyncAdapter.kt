package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.AggregatorGameSyncPort
import domain.aggregator.model.AggregatorGame
import domain.aggregator.model.AggregatorInfo
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient

/**
 * Pragmatic implementation for syncing games.
 */
class PragmaticGameSyncAdapter(aggregatorInfo: AggregatorInfo) : AggregatorGameSyncPort {

    private val config = PragmaticConfig(aggregatorInfo.config)
    private val client = PragmaticHttpClient(config)

    override suspend fun listGames(): Result<List<AggregatorGame>> {
        TODO("Not implemented: List games from Pragmatic")
    }
}
