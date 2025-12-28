package infrastructure.aggregator

import application.port.outbound.AggregatorAdapterFactory
import application.port.outbound.AggregatorAdapterRegistry
import infrastructure.aggregator.onegamehub.OneGameHubAdapterFactory
import infrastructure.aggregator.pateplay.PateplayAdapterFactory
import infrastructure.aggregator.pragmatic.PragmaticAdapterFactory
import domain.common.value.Aggregator

/**
 * Default implementation of AggregatorAdapterRegistry.
 * Manages multiple aggregator adapter factories.
 */
class AggregatorAdapterRegistryImpl(
    private val oneGameHubAdapterFactory: OneGameHubAdapterFactory,
    private val pragmaticAdapterFactory: PragmaticAdapterFactory,
    private val pateplayAdapterFactory: PateplayAdapterFactory
) : AggregatorAdapterRegistry {
    override fun getFactory(aggregator: Aggregator): AggregatorAdapterFactory? {
        return when (aggregator) {
            Aggregator.ONEGAMEHUB -> oneGameHubAdapterFactory
            Aggregator.PRAGMATIC -> pragmaticAdapterFactory
            Aggregator.PATEPLAY -> pateplayAdapterFactory
        }
    }
}
