package infrastructure.persistence.exposed.migration

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Table for tracking applied database migrations.
 */
object MigrationTable : Table("schema_migrations") {
    val version = varchar("version", 50)
    val description = varchar("description", 255)
    val appliedAt = datetime("applied_at")

    override val primaryKey = PrimaryKey(version)
}
