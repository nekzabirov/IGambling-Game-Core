package com.nekgamebling.application.usecase.aggregator

import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository
import com.nekgamebling.domain.common.error.DuplicateEntityError
import com.nekgamebling.shared.value.Aggregator
import java.util.UUID

/**
 * Use case for adding a new aggregator.
 */
class AddAggregatorUsecase(
    private val aggregatorRepository: AggregatorRepository
) {
    suspend operator fun invoke(
        identity: String,
        aggregator: Aggregator,
        config: Map<String, String>
    ): Result<AggregatorInfo> {
        if (aggregatorRepository.existsByIdentity(identity)) {
            return Result.failure(DuplicateEntityError("Aggregator", identity))
        }

        val aggregatorInfo = AggregatorInfo(
            id = UUID.randomUUID(),
            identity = identity,
            config = config,
            aggregator = aggregator,
            active = true
        )

        return try {
            val saved = aggregatorRepository.save(aggregatorInfo)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
