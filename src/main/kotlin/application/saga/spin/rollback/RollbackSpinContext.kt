package application.saga.spin.rollback

import application.saga.BaseSagaContext
import domain.session.model.Balance
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin

/**
 * Context for RollbackSpin saga.
 */
class RollbackSpinContext(
    val session: Session,
    val extRoundId: String,
    val transactionId: String,
    correlationId: String = transactionId
) : BaseSagaContext(correlationId = correlationId) {

    var round: Round? = null
    var originalSpin: Spin? = null
    var rollbackSpin: Spin? = null
    var refundRealAmount: Long = 0L
    var refundBonusAmount: Long = 0L
    var gameIdentity: String = ""
    var resultBalance: Balance? = null  // Balance after wallet refund

    val isFreeSpin: Boolean get() = originalSpin?.freeSpinId != null

    companion object {
        const val KEY_WALLET_REFUND_COMPLETED = "wallet_refund_completed"
    }
}
