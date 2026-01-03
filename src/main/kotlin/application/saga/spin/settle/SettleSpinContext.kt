package application.saga.spin.settle

import application.saga.BaseSagaContext
import domain.session.model.Balance
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import java.math.BigInteger

/**
 * Context for SettleSpin saga.
 */
class SettleSpinContext(
    val session: Session,
    val extRoundId: String,
    val transactionId: String,
    val freeSpinId: String?,
    val winAmount: BigInteger,
    correlationId: String = transactionId
) : BaseSagaContext(correlationId = correlationId) {

    var round: Round? = null
    var placeSpin: Spin? = null
    var settleSpin: Spin? = null
    var realAmount: BigInteger = BigInteger.ZERO
    var bonusAmount: BigInteger = BigInteger.ZERO
    var gameIdentity: String = ""
    var resultBalance: Balance? = null  // Balance after wallet operation

    val isFreeSpin: Boolean get() = freeSpinId != null

    companion object {
        const val KEY_WALLET_TX_COMPLETED = "wallet_tx_completed"
    }
}
