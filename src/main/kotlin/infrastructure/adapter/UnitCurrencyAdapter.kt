package infrastructure.adapter

import application.port.outbound.CurrencyAdapter
import shared.value.Currency
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Base currency adapter with common currency configurations.
 */
class UnitCurrencyAdapter : CurrencyAdapter {
    override suspend fun convertToSystem(
        amount: BigDecimal,
        currency: Currency
    ): BigInteger {
        return (amount * BigDecimal("100")).toBigInteger()
    }

    override suspend fun convertFromSystem(
        amount: BigInteger,
        currency: Currency
    ): BigDecimal {
        return amount.toBigDecimal() / BigDecimal("100")
    }

}
