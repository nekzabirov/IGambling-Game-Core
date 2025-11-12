package app.aggregator.base

interface IAggregatorConfig {
    fun parse(data: Map<String, String>)
}