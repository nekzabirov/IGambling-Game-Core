package com.nekgamebling.application.usecase.aggregator

import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository

/**
 * Use case for listing all active aggregators.
 */
class ListAllActiveAggregatorUsecase(
    private val aggregatorRepository: AggregatorRepository
) {
    suspend operator fun invoke(): List<AggregatorInfo> {
        return aggregatorRepository.findAllActive()
    }
}
