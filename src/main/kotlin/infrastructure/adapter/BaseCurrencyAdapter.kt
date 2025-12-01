package infrastructure.adapter

import application.port.outbound.CurrencyAdapter
import shared.value.Currency

/**
 * Base currency adapter with common currency configurations.
 */
class BaseCurrencyAdapter : CurrencyAdapter {
    private val supportedCurrencies = listOf(
        Currency("USD"),
        Currency("EUR"),
        Currency("GBP"),
        Currency("CAD"),
        Currency("AUD"),
        Currency("JPY"),
        Currency("CNY"),
        Currency("INR"),
        Currency("BRL"),
        Currency("RUB")
    )

    private val betLimits = mapOf(
        "USD" to (100 to 100000),
        "EUR" to (100 to 90000),
        "GBP" to (100 to 80000),
        "CAD" to (100 to 130000),
        "AUD" to (100 to 150000),
        "JPY" to (100 to 15000000),
        "CNY" to (100 to 700000),
        "INR" to (100 to 8000000),
        "BRL" to (100 to 500000),
        "RUB" to (100 to 10000000)
    )

    override suspend fun getSupportedCurrencies(): List<Currency> = supportedCurrencies

    override suspend fun isSupported(currency: Currency): Boolean =
        supportedCurrencies.any { it.value == currency.value }

    override suspend fun getMinBetAmount(currency: Currency): Int =
        betLimits[currency.value]?.first ?: 100

    override suspend fun getMaxBetAmount(currency: Currency): Int =
        betLimits[currency.value]?.second ?: 100000
}
