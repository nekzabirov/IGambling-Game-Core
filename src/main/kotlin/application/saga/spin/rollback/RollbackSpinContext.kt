package application.saga.spin.rollback

import application.saga.BaseSagaContext
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import java.math.BigInteger

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
    var refundRealAmount: BigInteger = BigInteger.ZERO
    var refundBonusAmount: BigInteger = BigInteger.ZERO
    var gameIdentity: String = ""

    val isFreeSpin: Boolean get() = originalSpin?.freeSpinId != null

    companion object {
        const val KEY_WALLET_REFUND_COMPLETED = "wallet_refund_completed"
    }
}
