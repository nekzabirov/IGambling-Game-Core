package application.port.outbound

import shared.value.Currency

/**
 * Port interface for currency operations.
 */
interface CurrencyAdapter {
    /**
     * Get list of supported currencies.
     */
    suspend fun getSupportedCurrencies(): List<Currency>

    /**
     * Check if a currency is supported.
     */
    suspend fun isSupported(currency: Currency): Boolean

    /**
     * Get minimum bet amount for a currency.
     */
    suspend fun getMinBetAmount(currency: Currency): Int

    /**
     * Get maximum bet amount for a currency.
     */
    suspend fun getMaxBetAmount(currency: Currency): Int
}
