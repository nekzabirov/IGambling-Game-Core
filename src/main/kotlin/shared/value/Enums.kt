package shared.value

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
    PRAGMATIC
}
