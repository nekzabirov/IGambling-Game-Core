package domain.collection.table

import core.db.AbstractTable
import core.value.ImageMap
import core.value.LocaleName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.json.jsonb

object CollectionTable : AbstractTable("collections") {
    val identity = varchar("identity", 100)
        .uniqueIndex()

    val name = jsonb<LocaleName>("name", Json.Default)

    val active = bool("active")
        .default(false)

    val order = integer("order")
        .default(100)

    val images = jsonb<ImageMap>("images", Json.Default)
        .default(ImageMap(emptyMap()))
}