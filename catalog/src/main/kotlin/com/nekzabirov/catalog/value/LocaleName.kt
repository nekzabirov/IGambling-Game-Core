package com.nekzabirov.catalog.value

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class LocaleName(val data: Map<String, String>)