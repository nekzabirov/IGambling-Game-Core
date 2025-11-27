package core.value

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Locale(val value: String)