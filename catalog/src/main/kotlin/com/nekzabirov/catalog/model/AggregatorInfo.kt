package com.nekzabirov.catalog.model

import com.nekzabirov.aggregators.value.Aggregator
import java.util.UUID

data class AggregatorInfo(
    val id: UUID,

    val identity: String,

    val config: Map<String, String>,

    val aggregator: Aggregator,

    val active: Boolean = true,
)
