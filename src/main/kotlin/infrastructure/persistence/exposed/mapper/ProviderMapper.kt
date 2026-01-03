package infrastructure.persistence.exposed.mapper

import domain.provider.model.Provider
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toProvider(): Provider = Provider(
    id = this[ProviderTable.id].value,
    identity = this[ProviderTable.identity],
    name = this[ProviderTable.name],
    images = this[ProviderTable.images],
    order = this[ProviderTable.order],
    aggregatorId = this[ProviderTable.aggregatorId]?.value,
    active = this[ProviderTable.active]
)
