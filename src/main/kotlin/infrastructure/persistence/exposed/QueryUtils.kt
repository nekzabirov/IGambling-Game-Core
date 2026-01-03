package infrastructure.persistence.exposed

import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.LikeEscapeOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.stringLiteral

/**
 * Utility functions for building Exposed SQL queries.
 * These provide common patterns for case-insensitive search and pagination.
 */
object QueryUtils {

    /**
     * Escapes special SQL LIKE pattern characters in a search query.
     * Escapes: % (wildcard), _ (single char), \ (escape char)
     */
    private fun escapeLikePattern(query: String): String {
        return query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Creates a case-insensitive LIKE pattern for partial matching.
     * Properly escapes special characters and adds wildcards.
     *
     * @param query The search query to match
     * @return Pattern string ready for LIKE operation (e.g., "%search%")
     */
    fun likePattern(query: String): String {
        val escaped = escapeLikePattern(query.trim())
        return "%$escaped%"
    }
}

/**
 * Creates a case-insensitive LIKE expression for a column.
 * Uses LOWER() on both column and pattern for database-agnostic case-insensitivity.
 *
 * @param query The search query
 * @return SQL expression for case-insensitive partial match
 */
fun Expression<String>.ilike(query: String): Op<Boolean> {
    val pattern = QueryUtils.likePattern(query).lowercase()
    return LikeEscapeOp(this.lowerCase(), stringLiteral(pattern), true, '\\')
}
