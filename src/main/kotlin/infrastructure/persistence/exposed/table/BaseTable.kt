package infrastructure.persistence.exposed.table

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Base table with common fields (id, createdAt, updatedAt).
 */
abstract class BaseTable(name: String) : UUIDTable(name) {
    val createdAt = datetime("created_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
    val updatedAt = datetime("updated_at").clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }
}
