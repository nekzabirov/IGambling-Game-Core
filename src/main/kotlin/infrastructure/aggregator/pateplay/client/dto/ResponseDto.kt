package infrastructure.aggregator.pateplay.client.dto

import kotlinx.serialization.Serializable

/**
 * Error object returned by PatePlay API when request fails.
 */
@Serializable
data class PateplayErrorDto(
    val code: String? = null,
    val message: String? = null
)

/**
 * Generic response from PatePlay API.
 * If 'error' object is present, the request failed.
 */
@Serializable
data class PateplayResponseDto(
    val error: PateplayErrorDto? = null
) {
    val isSuccess: Boolean get() = error == null
}
