package com.nekzabirov.aggregators.adapter

import com.nekzabirov.aggregators.adapter.onegamehub.OneGameHubAdapter
import com.nekzabirov.aggregators.adapter.onegamehub.model.OneGameHubConfig
import com.nekzabirov.aggregators.core.IAggregatorAdapter
import com.nekzabirov.aggregators.core.IAggregatorConfig
import com.nekzabirov.aggregators.value.Aggregator

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