package core.model

import core.value.Currency

data class Balance(
    val real: Int,

    val bonus: Int,

    val currency: Currency,
) {
    val totalAmount: Int
        get() = real + bonus
}
