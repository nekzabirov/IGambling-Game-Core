package application.saga.spin

import application.saga.BaseSagaContext
import domain.game.model.Game
import domain.session.model.Balance
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import java.math.BigInteger
import java.util.UUID

/**
 * Context for PlaceSpin saga holding all intermediate state.
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
        const val KEY_GAME = "game"
        const val KEY_ROUND = "round"
        const val KEY_SPIN = "spin"
        const val KEY_BALANCE = "balance"
        const val KEY_WALLET_TX_COMPLETED = "wallet_tx_completed"
        const val KEY_ROUND_CREATED = "round_created"
    }
}
