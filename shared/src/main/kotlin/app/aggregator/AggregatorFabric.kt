package app.aggregator

import app.aggregator.onegamehub.OneGameHubAdapter
import app.aggregator.onegamehub.model.OneGameHubConfig
import domain.adapter.aggregator.IAggregatorAdapter
import domain.adapter.aggregator.IAggregatorConfig
import domain.value.Aggregator

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