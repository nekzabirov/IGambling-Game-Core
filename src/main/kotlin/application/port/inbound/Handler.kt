package application.port.inbound

/**
 * Marker interface for all queries.
 * Queries are read operations that do not modify state.
 *
 * @param R The result type of the query
 */
interface Query<R>

/**
 * Marker interface for all commands.
 * Commands are write operations that modify state.
 *
 * @param R The result type of the command
 */
interface Command<R>

/**
 * Handler for query operations.
 * Implementations should be optimized for read performance.
 * Uses direct database access via Exposed DSL.
 *
 * @param Q The query type
 * @param R The result type
 */
interface QueryHandler<Q : Query<R>, R> {
    /**
     * Execute the query and return the result.
     * Queries do not return Result<> wrapper since they are pure reads.
     * Returns nullable for single-entity queries (null = not found).
     */
    suspend fun handle(query: Q): R
}

/**
 * Handler for command operations.
 * Implementations orchestrate writes, events, and adapter calls.
 *
 * @param C The command type
 * @param R The result type
 */
interface CommandHandler<C : Command<R>, R> {
    /**
     * Execute the command and return the result.
     * Commands return Result<> to handle potential failures.
     */
    suspend fun handle(command: C): Result<R>
}
