package infrastructure.aggregator.onegamehub.adapter

import application.port.outbound.CurrencyAdapter
import shared.value.Currency
import java.math.BigDecimal
import java.math.RoundingMode

class OneGameHubCurrencyAdapter(private val currencyAdapter: CurrencyAdapter) {
    suspend fun convertSystemToProvider(amount: Long, currency: Currency): Long {
        val original = currencyAdapter.convertFromSystem(amount, currency)

        return (original * BigDecimal("100")).toLong()
    }

    suspend fun convertProviderToSystem(amount: Long, currency: Currency): Long {
        val original = BigDecimal(amount).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)

        return currencyAdapter.convertToSystem(original, currency)
    }
}