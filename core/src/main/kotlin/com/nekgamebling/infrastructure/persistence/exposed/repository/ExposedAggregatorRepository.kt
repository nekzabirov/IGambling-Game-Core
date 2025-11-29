package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toAggregatorInfo
import com.nekgamebling.infrastructure.persistence.exposed.table.AggregatorInfoTable
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Exposed implementation of AggregatorRepository.
 */
class ExposedAggregatorRepository : BaseExposedRepositoryWithIdentity<AggregatorInfo, AggregatorInfoTable>(AggregatorInfoTable), AggregatorRepository {

    override fun ResultRow.toEntity(): AggregatorInfo = toAggregatorInfo()

    override suspend fun findByAggregator(aggregator: Aggregator): AggregatorInfo? =
        findOneBy(AggregatorInfoTable.aggregator, aggregator)

    override suspend fun save(aggregatorInfo: AggregatorInfo): AggregatorInfo = newSuspendedTransaction {
        val id = AggregatorInfoTable.insertAndGetId {
            it[identity] = aggregatorInfo.identity
            it[config] = aggregatorInfo.config
            it[aggregator] = aggregatorInfo.aggregator
            it[active] = aggregatorInfo.active
        }
        aggregatorInfo.copy(id = id.value)
    }

    override suspend fun update(aggregatorInfo: AggregatorInfo): AggregatorInfo = newSuspendedTransaction {
        AggregatorInfoTable.update({ AggregatorInfoTable.id eq aggregatorInfo.id }) {
            it[identity] = aggregatorInfo.identity
            it[config] = aggregatorInfo.config
            it[aggregator] = aggregatorInfo.aggregator
            it[active] = aggregatorInfo.active
        }
        aggregatorInfo
    }

    override suspend fun findAll(pageable: Pageable): Page<AggregatorInfo> = findAllPaged(pageable)

    override suspend fun findAllActive(): List<AggregatorInfo> = findAllBy(AggregatorInfoTable.active, true)
}
