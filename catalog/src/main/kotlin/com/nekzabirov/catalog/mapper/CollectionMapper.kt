package com.nekzabirov.catalog.mapper


import com.nekzabirov.catalog.model.Collection
import com.nekzabirov.catalog.table.CollectionTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCollection() = Collection(
    id = this[CollectionTable.id].value,

    identity = this[CollectionTable.identity],

    name = this[CollectionTable.name],

    order = this[CollectionTable.order],

    active = this[CollectionTable.active],

    images = this[CollectionTable.images],
)