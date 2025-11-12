package domain.mapper

import domain.model.AggregatorInfo
import domain.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toAggregatorModel() = _root_ide_package_.domain.model.AggregatorInfo(
    id = this[AggregatorInfoTable.id].value,

    identity = this[AggregatorInfoTable.identity],

    config = this[AggregatorInfoTable.config],

    aggregator = this[AggregatorInfoTable.aggregator],

    active = this[AggregatorInfoTable.active]
)