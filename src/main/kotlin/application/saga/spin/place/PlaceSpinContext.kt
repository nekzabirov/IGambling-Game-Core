package application.saga.spin.place

import application.saga.BaseSagaContext
import domain.game.model.Game
import domain.session.model.Balance
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import java.math.BigInteger

/**
 * Context for PlaceSpin saga.
 * Holds all data needed during saga execution.
 */
class PlaceSpinContext(
    val session: Session,
    val gameSymbol: String,
    val extRoundId: String,
    val transactionId: String,
    val freeSpinId: String?,
    val amount: BigInteger,
    correlationId: String = transactionId
) : BaseSagaContext(correlationId = correlationId) {

    // Intermediate results stored during saga execution
    var game: Game? = null
    var round: Round? = null
    var balance: Balance? = null
    var betLimit: BigInteger? = null
    var spin: Spin? = null
    var betRealAmount: BigInteger = BigInteger.ZERO
    var betBonusAmount: BigInteger = BigInteger.ZERO

    val isFreeSpin: Boolean get() = freeSpinId != null

    companion object {
        const val KEY_ROUND_CREATED = "round_created"
        const val KEY_WALLET_TX_COMPLETED = "wallet_tx_completed"
    }
}
