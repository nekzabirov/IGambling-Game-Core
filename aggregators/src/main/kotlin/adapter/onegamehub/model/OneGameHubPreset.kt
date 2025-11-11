package com.nekzabirov.aggregators.adapter.onegamehub.model

import com.nekzabirov.aggregators.core.IAggregatorPreset

data class OneGameHubPreset(
    override var quantity: Int,

    override var betAmount: Int,

    override var minQuantity: Int = 1,

    override var minBetAmount: Int = 10,

    var lines: Int = 10
) : IAggregatorPreset {
    override fun toMap(): Map<String, Any> = mapOf(
        "quantity" to quantity,
        "bet_amount" to betAmount,
        "min_quantity" to minQuantity,
        "min_bet_amount" to minBetAmount,
        "lines" to lines,
    )

    override fun fromMap(map: Map<String, Any>) {
        quantity = map.getOrDefault("quantity", 1) as Int

        betAmount = map.getOrDefault("betAmount", 10) as Int

        lines = map.getOrDefault("lines", 10) as Int
    }
}
