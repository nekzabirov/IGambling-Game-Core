package domain.value

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class LocaleName(val data: Map<String, String>)