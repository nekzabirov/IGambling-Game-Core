package application.usecase.spin

import domain.session.model.RoundDetails
import domain.session.repository.RoundFilter
import domain.session.repository.RoundRepository
import shared.value.Page
import shared.value.Pageable

/**
 * Use case for retrieving rounds with details (aggregated amounts, game info).
 */
class GetRoundsDetailsUsecase(
    private val roundRepository: RoundRepository
) {
    /**
     * Get rounds with details using pagination and optional filters.
     */
    suspend operator fun invoke(
        pageable: Pageable,
        filter: RoundFilter = RoundFilter.EMPTY
    ): Page<RoundDetails> {
        return roundRepository.findAllWithDetails(pageable, filter)
    }

    /**
     * Get rounds with details using DSL-style filter builder.
     */
    suspend operator fun invoke(
        pageable: Pageable,
        filterBuilder: RoundFilter.Builder.() -> Unit
    ): Page<RoundDetails> {
        val filter = RoundFilter.builder().apply(filterBuilder).build()
        return invoke(pageable, filter)
    }
}
