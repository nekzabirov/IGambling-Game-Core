package app.aggregator.base

interface IAggregatorPreset {
    val quantity: Int

    val betAmount: Int

    val minQuantity: Int

    val minBetAmount: Int

    fun toMap(): Map<String, Any>

    fun fromMap(map: Map<String, Any>)
}
