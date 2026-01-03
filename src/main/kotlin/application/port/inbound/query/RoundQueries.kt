package application.port.inbound.query

import application.port.inbound.Query
import domain.game.model.GameWithDetails
import domain.session.model.RoundDetails
import domain.session.repository.RoundFilter
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import shared.value.Currency
import shared.value.Page
import shared.value.Pageable
import java.math.BigInteger
import java.util.UUID

/**
 * Read model for round details with aggregated spin data.
 */
@Serializable
data class RoundDetailsReadModel(
    @Serializable(with = shared.serializer.UUIDSerializer::class)
    val id: UUID,
    @Contextual
    val placeAmount: BigInteger,
    @Contextual
    val settleAmount: BigInteger,
    val freeSpinId: String?,
    val currency: String,
    val gameIdentity: String,
    val gameName: String,
    val providerName: String,
    val isFinished: Boolean,
    val createdAt: LocalDateTime,
    val finishedAt: LocalDateTime?
) {
    companion object {
        fun from(details: RoundDetails) = RoundDetailsReadModel(
            id = details.id,
            placeAmount = details.placeAmount,
            settleAmount = details.settleAmount,
            freeSpinId = details.freeSpinId,
            currency = details.currency.value,
            gameIdentity = details.game.identity,
            gameName = details.game.name,
            providerName = details.game.provider.name,
            isFinished = details.isFinished,
            createdAt = details.createdAt,
            finishedAt = details.finishedAt
        )
    }
}

/**
 * Query to get round details with pagination and filtering.
 */
data class GetRoundsDetailsQuery(
    val pageable: Pageable,
    val filter: RoundFilter = RoundFilter.EMPTY
) : Query<Page<RoundDetailsReadModel>>
