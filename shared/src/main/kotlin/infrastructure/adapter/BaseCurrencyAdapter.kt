package infrastructure.adapter

import adapter.CurrencyAdapter
import core.value.Currency

class BaseCurrencyAdapter : CurrencyAdapter {
    override fun convertToSystem(currency: Currency, value: Float): Int {
        return (value * 100).toInt()
    }

    override fun convertFromSystem(currency: Currency, value: Int): Float {
        return value.toFloat() / 100
    }
}