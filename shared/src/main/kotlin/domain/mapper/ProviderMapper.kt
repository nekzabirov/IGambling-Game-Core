package domain.mapper

import domain.model.Provider
import domain.table.ProviderTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toProvider() = _root_ide_package_.domain.model.Provider(
    id = this[ProviderTable.id].value,

    identity = this[ProviderTable.identity],

    name = this[ProviderTable.name],

    order = this[ProviderTable.order],

    aggregatorId = this[ProviderTable.aggregatorId].value,

    active = this[ProviderTable.active],

    images = this[ProviderTable.images]
)