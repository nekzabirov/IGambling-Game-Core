package com.nekzabirov.catalog.model

import com.nekzabirov.catalog.value.ImageMap
import com.nekzabirov.catalog.value.LocaleName
import java.util.UUID

data class Collection(
    val id: UUID = UUID.randomUUID(),

    val identity: String,

    val name: LocaleName,

    val active: Boolean = true,

    val order: Int = 100,

    val images: ImageMap,
)
