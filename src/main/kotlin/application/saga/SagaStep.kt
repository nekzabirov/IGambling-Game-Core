package application.saga

/**
 * Represents a single step in a saga with its compensation action.
 *
 * Each step defines:
 * - Forward action (execute)
 * - Compensation action (compensate) - called when a later step fails
 *
 * @param C The type of saga context this step operates on
 */
interface SagaStep<C : SagaContext> {
    /** Unique identifier for this step */
    val stepId: String

    /** Human-readable name for logging/audit */
    val stepName: String

    /** Whether this step requires compensation on rollback */
    val requiresCompensation: Boolean get() = true

    /**
     * Execute the forward action of this step.
     *
     * @param context Shared saga context
     * @return Result indicating success or failure with error
     */
    suspend fun execute(context: C): Result<Unit>

    /**
     * Execute the compensation (rollback) action.
     * Called when a subsequent step fails.
     *
     * @param context Shared saga context
     * @return Result indicating compensation success or failure
     */
    suspend fun compensate(context: C): Result<Unit>
}

/**
 * Abstract base class providing common step functionality.
 * Use this for steps that don't need compensation.
 */
abstract class AbstractSagaStep<C : SagaContext>(
    override val stepId: String,
    override val stepName: String,
    override val requiresCompensation: Boolean = true
) : SagaStep<C> {

    override suspend fun compensate(context: C): Result<Unit> {
        // Default: no-op compensation
        return Result.success(Unit)
    }
}

/**
 * A saga step that doesn't require compensation.
 * Useful for validation steps that don't make changes.
 */
abstract class ValidationStep<C : SagaContext>(
    stepId: String,
    stepName: String
) : AbstractSagaStep<C>(stepId, stepName, requiresCompensation = false)
