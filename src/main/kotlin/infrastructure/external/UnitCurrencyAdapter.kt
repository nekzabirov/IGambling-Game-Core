package infrastructure.external

import application.port.outbound.CurrencyAdapter
import shared.value.Currency
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Base currency adapter with common currency configurations.
 */
class UnitCurrencyAdapter : CurrencyAdapter {
    override suspend fun convertToSystem(
        amount: BigDecimal,
        currency: Currency
    ): Long {
        return amount.multiply(BigDecimal("100")).toLong()
    }

    override suspend fun convertFromSystem(
        amount: Long,
        currency: Currency
    ): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    }

}
