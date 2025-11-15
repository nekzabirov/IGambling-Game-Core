package adapter

import core.value.Currency

interface CurrencyAdapter {
    fun convertToSystem(currency: Currency, value: Float): Int

    fun convertFromSystem(currency: Currency, value: Int): Float
}