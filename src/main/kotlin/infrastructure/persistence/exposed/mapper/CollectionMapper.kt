package infrastructure.persistence.exposed.mapper

import domain.collection.model.Collection
import infrastructure.persistence.exposed.table.CollectionTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCollection(): Collection = Collection(
    id = this[CollectionTable.id].value,
    identity = this[CollectionTable.identity],
    name = this[CollectionTable.name],
    active = this[CollectionTable.active],
    order = this[CollectionTable.order]
)
