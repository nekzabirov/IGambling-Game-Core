package core.value

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Currency(val value: String)