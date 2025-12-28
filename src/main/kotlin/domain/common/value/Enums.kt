package domain.common.value

import kotlinx.serialization.Serializable

/**
 * Platform type for game variants.
 */
enum class Platform {
    DESKTOP,
    MOBILE,
    DOWNLOAD
}

/**
 * Spin type representing the state of a spin transaction.
 */
enum class SpinType {
    PLACE,
    SETTLE,
    ROLLBACK
}

/**
 * Supported game aggregators.
 */
enum class Aggregator {
    ONEGAMEHUB,
    PRAGMATIC,
    PATEPLAY
}

/**
 * Locale value object representing a locale code (e.g., "en", "de").
 */
@Serializable
@JvmInline
value class Locale(val value: String) {
    init {
        require(value.isNotBlank()) { "Locale code cannot be blank" }
    }
}
