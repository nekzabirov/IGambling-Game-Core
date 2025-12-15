package infrastructure.aggregator.shared

import application.port.outbound.CurrencyAdapter
import shared.value.Currency
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Shared currency adapter wrapper for aggregators that use the standard conversion
 * (system cents to provider decimal format using the base CurrencyAdapter).
 *
 * Used by Pateplay and Pragmatic aggregators.
 */
class ProviderCurrencyAdapter(private val currencyAdapter: CurrencyAdapter) {
    suspend fun convertSystemToProvider(amount: BigInteger, currency: Currency): BigDecimal {
        return currencyAdapter.convertFromSystem(amount, currency)
    }

    suspend fun convertProviderToSystem(amount: BigDecimal, currency: Currency): BigInteger {
        return currencyAdapter.convertToSystem(amount, currency)
    }
}
