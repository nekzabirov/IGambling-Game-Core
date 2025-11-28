package com.nekgamebling.infrastructure.aggregator

import com.nekgamebling.application.port.outbound.AggregatorAdapterFactory
import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.infrastructure.aggregator.onegamehub.OneGameHubAdapterFactory
import com.nekgamebling.shared.value.Aggregator

/**
 * Default implementation of AggregatorAdapterRegistry.
 * Manages multiple aggregator adapter factories.
 */
class AggregatorAdapterRegistryImpl : AggregatorAdapterRegistry {
    private val factories = mutableMapOf<Aggregator, AggregatorAdapterFactory>()

    override fun getFactory(aggregator: Aggregator): AggregatorAdapterFactory? {
        return when (aggregator) {
            Aggregator.ONEGAMEHUB -> OneGameHubAdapterFactory()
        }
    }

    override fun register(factory: AggregatorAdapterFactory) {
        Aggregator.entries.filter { factory.supports(it) }.forEach { aggregator ->
            factories[aggregator] = factory
        }
    }

    /**
     * Register multiple factories at once.
     */
    fun registerAll(vararg factoryList: AggregatorAdapterFactory) {
        factoryList.forEach { register(it) }
    }
}
