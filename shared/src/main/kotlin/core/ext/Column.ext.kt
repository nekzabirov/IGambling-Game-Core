package core.ext

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.append
import org.jetbrains.exposed.sql.*

fun <T> Column<T>.toJsonText(): Expression<String> =
    object : Expression<String>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder.append(this@toJsonText, "::text")
        }
    }

infix fun <T> Expression<T>.ilike(pattern: String): Op<Boolean> {
    val patternParam = stringParam(pattern)

    return object : Op<Boolean>() {
        override fun toQueryBuilder(queryBuilder: QueryBuilder) {
            queryBuilder.append(this@ilike, " ILIKE ", patternParam)
        }
    }
}