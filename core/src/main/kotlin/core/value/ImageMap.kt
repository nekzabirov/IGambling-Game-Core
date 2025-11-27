package core.value

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ImageMap(val data: Map<String, String>)