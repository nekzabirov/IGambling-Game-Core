package application.saga

import kotlinx.serialization.Serializable

/**
 * Represents the possible states of a saga execution.
 */
@Serializable
enum class SagaState {
    /** Saga created but not started */
    PENDING,

    /** Saga is executing steps */
    RUNNING,

    /** Saga failed, executing compensations */
    COMPENSATING,

    /** All steps completed successfully */
    COMPLETED,

    /** All compensations executed after failure */
    COMPENSATED,

    /** Compensation also failed (requires manual intervention) */
    FAILED
}

/**
 * Represents the result of a saga step execution.
 */
@Serializable
enum class StepStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    COMPENSATED,
    COMPENSATION_FAILED
}
