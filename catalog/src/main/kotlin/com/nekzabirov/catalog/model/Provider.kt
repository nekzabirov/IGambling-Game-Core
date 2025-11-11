package com.nekzabirov.catalog.model

import com.nekzabirov.catalog.value.ImageMap
import java.util.UUID

data class Provider(
    val id: UUID,

    val identity: String,

    val name: String,

    val images: ImageMap,

    val order: Int = 100,

    val aggregatorId: UUID? = null,

    val active: Boolean = true
)
