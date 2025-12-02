package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.CurrencyAdapter
import shared.value.Currency
import java.math.BigDecimal
import java.math.BigInteger

class PragmaticCurrencyAdapter(private val currencyAdapter: CurrencyAdapter) {
    suspend fun convertSystemToProvider(amount: BigInteger, currency: Currency): BigDecimal {
        return currencyAdapter.convertFromSystem(amount, currency)
    }

    suspend fun convertProviderToSystem(amount: BigDecimal, currency: Currency): BigInteger {
        return currencyAdapter.convertToSystem(amount, currency)
    }
}
