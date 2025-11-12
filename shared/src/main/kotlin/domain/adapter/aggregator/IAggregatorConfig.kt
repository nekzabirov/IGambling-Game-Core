package domain.adapter.aggregator

interface IAggregatorConfig {
    fun parse(data: Map<String, String>)
}