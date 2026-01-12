package domain.session.model

import domain.game.model.GameWithDetails
import kotlinx.datetime.LocalDateTime
import shared.value.Currency
import java.util.UUID

/**
 * Round details read model with aggregated spin amounts and game information.
 */
data class RoundDetails(
    val id: UUID,
    val placeAmount: Long,
    val settleAmount: Long,
    val freeSpinId: String?,
    val currency: Currency,
    val game: GameWithDetails,
    val isFinished: Boolean,
    val createdAt: LocalDateTime,
    val finishedAt: LocalDateTime?
)
