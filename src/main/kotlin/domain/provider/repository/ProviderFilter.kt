package domain.provider.repository

/**
 * Filter criteria for provider queries.
 */
data class ProviderFilter(
    val query: String = "",
    val active: Boolean? = null
)
