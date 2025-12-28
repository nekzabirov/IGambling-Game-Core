package application.saga

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Base interface for saga execution context.
 * Holds shared data across all saga steps.
 */
interface SagaContext {
    /** Unique identifier for this saga execution */
    val sagaId: UUID

    /** Correlation ID for tracing (e.g., transaction ID) */
    val correlationId: String

    /** When the saga started */
    val startedAt: Long

    /** Store intermediate results between steps */
    fun <T : Any> put(key: String, value: T)

    /** Retrieve intermediate results */
    fun <T : Any> get(key: String): T?

    /** Check if a key exists */
    fun has(key: String): Boolean

    /** Remove a value */
    fun remove(key: String): Any?
}

/**
 * Base implementation of SagaContext.
 */
abstract class BaseSagaContext(
    override val sagaId: UUID = UUID.randomUUID(),
    override val correlationId: String,
    override val startedAt: Long = System.currentTimeMillis()
) : SagaContext {

    private val data = ConcurrentHashMap<String, Any>()

    override fun <T : Any> put(key: String, value: T) {
        data[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? = data[key] as? T

    override fun has(key: String): Boolean = data.containsKey(key)

    override fun remove(key: String): Any? = data.remove(key)
}
