package application.port.outbound

import shared.value.Currency
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Port interface for currency operations.
 */
interface CurrencyAdapter {
    suspend fun convertToSystem(amount: BigDecimal, currency: Currency): BigInteger

    suspend fun convertFromSystem(amount: BigInteger, currency: Currency): BigDecimal
}
