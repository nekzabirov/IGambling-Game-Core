package app.aggregator.onegamehub

import domain.value.Currency


internal object OneGameHubCurrencyAdapter {

    fun convertToAggregator(currency: domain.value.Currency, value: Int): Float {
        return value.toFloat()
    }

}