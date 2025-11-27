package infrastructure.aggregator

import infrastructure.aggregator.onegamehub.adapter.OneGameHubAdapter
import infrastructure.aggregator.onegamehub.model.OneGameHubConfig
import domain.aggregator.adapter.IAggregatorAdapter
import domain.aggregator.adapter.IAggregatorConfig
import domain.aggregator.model.Aggregator

object AggregatorFabric {

    fun createConfig(configData: Map<String, String>, aggregator: Aggregator): IAggregatorConfig {
        val config = when (aggregator) {
            Aggregator.ONEGAMEHUB -> OneGameHubConfig()
        }

        config.parse(configData)

        return config
    }

    fun createAdapter(configData: Map<String, String>, aggregator: Aggregator): IAggregatorAdapter {
        val config = createConfig(configData, aggregator)

        return when (aggregator) {
            Aggregator.ONEGAMEHUB -> OneGameHubAdapter(config as OneGameHubConfig)
        }
    }

}