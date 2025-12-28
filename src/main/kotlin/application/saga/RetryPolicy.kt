package application.saga

import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Retry policy configuration for saga steps.
 */
data class RetryPolicy(
    val maxAttempts: Int,
    val delayMs: Long,
    val retryableErrors: Set<String> = emptySet()
) {
    /**
     * Determine if an error should trigger a retry.
     */
    fun shouldRetry(error: Throwable?): Boolean {
        if (error == null) return false

        // Always retry transient network errors
        if (error is SocketTimeoutException) return true
        if (error is ConnectException) return true

        // Check configured retryable errors
        return retryableErrors.contains(error::class.simpleName)
    }

    companion object {
        /**
         * Default retry policy for most operations.
         * 3 attempts with 1 second delay.
         */
        fun default() = RetryPolicy(
            maxAttempts = 3,
            delayMs = 1000L,
            retryableErrors = setOf(
                "SocketTimeoutException",
                "ConnectException",
                "UnknownHostException"
            )
        )

        /**
         * No retry policy.
         * Use for operations that should fail immediately.
         */
        fun noRetry() = RetryPolicy(
            maxAttempts = 1,
            delayMs = 0L,
            retryableErrors = emptySet()
        )

        /**
         * Aggressive retry policy for critical operations.
         * 5 attempts with exponential backoff potential.
         */
        fun aggressive() = RetryPolicy(
            maxAttempts = 5,
            delayMs = 2000L,
            retryableErrors = setOf(
                "SocketTimeoutException",
                "ConnectException",
                "UnknownHostException",
                "ServiceUnavailableException"
            )
        )
    }
}
