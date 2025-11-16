package core.model

import core.value.Currency

data class Balance(
    val realAmount: Int,

    val bonusAmount: Int,

    val currency: Currency,
) {
    val totalAmount: Int
        get() = realAmount + bonusAmount
}
