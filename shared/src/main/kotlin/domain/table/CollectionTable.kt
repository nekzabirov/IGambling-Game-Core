package domain.table

import domain.table.base.AbstractTable
import domain.value.ImageMap
import domain.value.LocaleName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object CollectionTable : domain.table.base.AbstractTable("collections") {
    val identity = varchar("identity", 100)
        .uniqueIndex()

    val name = jsonb<domain.value.LocaleName>("name", Json.Default)

    val active = bool("active")
        .default(false)

    val order = integer("order")
        .default(100)

    val images = jsonb<domain.value.ImageMap>("images", Json.Default)
        .default(_root_ide_package_.domain.value.ImageMap(emptyMap()))
}