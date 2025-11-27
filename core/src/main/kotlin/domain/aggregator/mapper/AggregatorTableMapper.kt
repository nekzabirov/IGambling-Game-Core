package domain.aggregator.mapper

import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAggregatorModel() = AggregatorInfo(
    id = this[AggregatorInfoTable.id].value,

    identity = this[AggregatorInfoTable.identity],

    config = this[AggregatorInfoTable.config],

    aggregator = this[AggregatorInfoTable.aggregator],

    active = this[AggregatorInfoTable.active]
)