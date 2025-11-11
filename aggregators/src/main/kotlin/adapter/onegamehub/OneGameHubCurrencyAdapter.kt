package com.nekzabirov.aggregators.adapter.onegamehub

import com.nekzabirov.aggregators.value.Currency

internal object OneGameHubCurrencyAdapter {

    fun convertToAggregator(currency: Currency, value: Int): Float {
        return value.toFloat()
    }

}