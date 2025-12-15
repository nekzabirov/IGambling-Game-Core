package infrastructure.adapter

import application.port.outbound.CurrencyAdapter
import shared.value.Currency
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Base currency adapter with common currency configurations.
 */
class UnitCurrencyAdapter : CurrencyAdapter {
    override suspend fun convertToSystem(
        amount: BigDecimal,
        currency: Currency
    ): BigInteger {
        return amount.multiply(BigDecimal("100")).toBigInteger()
    }

    override suspend fun convertFromSystem(
        amount: BigInteger,
        currency: Currency
    ): BigDecimal {
        return amount.toBigDecimal().divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    }

}
