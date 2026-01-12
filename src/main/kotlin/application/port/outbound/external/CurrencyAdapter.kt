package application.port.outbound

import shared.value.Currency
import java.math.BigDecimal

/**
 * Port interface for currency operations.
 */
interface CurrencyAdapter {
    suspend fun convertToSystem(amount: BigDecimal, currency: Currency): Long

    suspend fun convertFromSystem(amount: Long, currency: Currency): BigDecimal
}
