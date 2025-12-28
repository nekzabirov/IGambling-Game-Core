package shared.value

import domain.common.value.Locale
import kotlinx.serialization.Serializable

/**
 * Currency value object representing a currency code (e.g., "USD", "EUR").
 */
@Serializable
@JvmInline
value class Currency(val value: String) {
    init {
        require(value.isNotBlank()) { "Currency code cannot be blank" }
    }
}

/**
 * Session token value object.
 */
@Serializable
@JvmInline
value class SessionToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Session token cannot be blank" }
    }
}

/**
 * Image map containing image URLs keyed by type (e.g., "thumbnail", "banner").
 */
@Serializable
@JvmInline
value class ImageMap(val data: Map<String, String>) {
    companion object {
        val EMPTY = ImageMap(emptyMap())
    }
}

/**
 * Localized name map containing names keyed by locale.
 */
@Serializable
@JvmInline
value class LocaleName(val data: Map<String, String>) {
    fun get(locale: Locale): String? = data[locale.value]
    fun get(locale: String): String? = data[locale]

    companion object {
        val EMPTY = LocaleName(emptyMap())
    }
}
