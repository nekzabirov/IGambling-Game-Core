package domain.aggregator.adapter

interface IAggregatorConfig {
    fun parse(data: Map<String, String>)
}