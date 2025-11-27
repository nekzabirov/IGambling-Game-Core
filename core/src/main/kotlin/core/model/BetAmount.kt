package core.model

import core.value.Currency

data class BetAmount(
    val real: Int,
    val bonus: Int,
    val currency: Currency,
)
