package infrastructure.persistence.exposed.table

import org.jetbrains.exposed.sql.Column

/**
 * Interface for tables that have an identity column.
 */
interface IdentityTable {
    val identity: Column<String>
}
