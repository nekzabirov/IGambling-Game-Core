package com.nekzabirov.catalog.mapper

import com.nekzabirov.catalog.model.Provider
import com.nekzabirov.catalog.table.ProviderTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toProvider() = Provider(
    id = this[ProviderTable.id].value,

    identity = this[ProviderTable.identity],

    name = this[ProviderTable.name],

    order = this[ProviderTable.order],

    aggregatorId = this[ProviderTable.aggregatorId].value,

    active = this[ProviderTable.active],

    images = this[ProviderTable.images]
)