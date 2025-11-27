package domain.aggregator.adapter

interface IAggregatorPreset {
    fun toMap(): Map<String, PresetParam>

    fun pushValue(map: Map<String, Int>)

    fun isValid(): Boolean
}

abstract class BaseFreespinPreset : IAggregatorPreset {
    val quantity = PresetParam()

    val betAmount = PresetParam()

    override fun toMap() = mapOf(
        "quantity" to quantity,
        "bet_amount" to betAmount,
    )

    override fun pushValue(map: Map<String, Int>) {
        quantity.value = map["quantity"]
        betAmount.value = map["bet_amount"]
    }

    override fun isValid(): Boolean {
        if (quantity.value == null || quantity.value!! < quantity.minimal!!) {
            return false
        }

        if (betAmount.value == null || betAmount.value!! < betAmount.minimal!!) {
            return false
        }

        return true
    }
}

data class PresetParam(
    var value: Int? = null,
    var default: Int? = null,
    var minimal: Int? = null,
    var maximum: Int? = null
)