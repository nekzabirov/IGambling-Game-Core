package infrastructure.aggregator.pragmatic

import application.port.outbound.AggregatorAdapterFactory
import application.port.outbound.AggregatorFreespinPort
import application.port.outbound.AggregatorGameSyncPort
import application.port.outbound.AggregatorLaunchUrlPort
import domain.aggregator.model.AggregatorInfo
import infrastructure.aggregator.pragmatic.adapter.PragmaticCurrencyAdapter
import infrastructure.aggregator.pragmatic.adapter.PragmaticFreespinAdapter
import infrastructure.aggregator.pragmatic.adapter.PragmaticGameSyncAdapter
import infrastructure.aggregator.pragmatic.adapter.PragmaticLaunchUrlAdapter
import shared.value.Aggregator

/**
 * Factory for creating Pragmatic aggregator adapters.
 */
class PragmaticAdapterFactory(private val providerCurrencyAdapter: PragmaticCurrencyAdapter) : AggregatorAdapterFactory {

    override fun supports(aggregator: Aggregator): Boolean {
        return aggregator == Aggregator.PRAGMATIC
    }

    override fun createLaunchUrlAdapter(aggregatorInfo: AggregatorInfo): AggregatorLaunchUrlPort {
        return PragmaticLaunchUrlAdapter(aggregatorInfo)
    }

    override fun createFreespinAdapter(aggregatorInfo: AggregatorInfo): AggregatorFreespinPort {
        return PragmaticFreespinAdapter(aggregatorInfo, providerCurrencyAdapter)
    }

    override fun createGameSyncAdapter(aggregatorInfo: AggregatorInfo): AggregatorGameSyncPort {
        return PragmaticGameSyncAdapter(aggregatorInfo)
    }
}
