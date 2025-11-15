package infrastructure.aggregator.onegamehub.adapter

import adapter.CurrencyAdapter
import core.value.Currency
import org.koin.core.component.KoinComponent

internal object OneGameHubCurrencyAdapter: KoinComponent {
    private val currencyAdapter = getKoin().get<CurrencyAdapter>()

    fun convertToAggregator(currency: Currency, value: Int): Int {
        val realAmount = currencyAdapter.convertFromSystem(currency, value)

        return (realAmount * 100).toInt()
    }

    fun convertFromAggregator(currency: Currency, value: Int): Int {
        val realAmount = value / 100f

        return currencyAdapter.convertToSystem(currency, realAmount)
    }
}