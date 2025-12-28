package application.saga.spin.end

import application.saga.BaseSagaContext
import domain.session.model.Round
import domain.session.model.Session

/**
 * Context for EndSpin saga.
 */
class EndSpinContext(
    val session: Session,
    val extRoundId: String,
    val freeSpinId: String?,
    correlationId: String = "${session.id}_$extRoundId"
) : BaseSagaContext(correlationId = correlationId) {

    var round: Round? = null
    var gameIdentity: String = ""
}
