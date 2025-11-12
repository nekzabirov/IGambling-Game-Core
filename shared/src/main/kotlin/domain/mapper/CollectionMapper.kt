package domain.mapper


import domain.model.Collection
import domain.table.CollectionTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCollection() = _root_ide_package_.domain.model.Collection(
    id = this[CollectionTable.id].value,

    identity = this[CollectionTable.identity],

    name = this[CollectionTable.name],

    order = this[CollectionTable.order],

    active = this[CollectionTable.active],

    images = this[CollectionTable.images],
)