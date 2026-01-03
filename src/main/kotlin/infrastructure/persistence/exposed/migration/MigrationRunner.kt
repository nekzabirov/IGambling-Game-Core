package infrastructure.persistence.exposed.migration

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Database migration runner.
 * Applies SQL migrations in order, tracking which have been applied.
 */
object MigrationRunner {
    private val logger = LoggerFactory.getLogger(MigrationRunner::class.java)

    /**
     * Run all pending migrations.
     */
    fun migrate() {
        logger.info("Starting database migration check...")

        transaction {
            SchemaUtils.create(MigrationTable)
        }

        val appliedMigrations = getAppliedMigrations()
        logger.info("Already applied migrations: $appliedMigrations")

        val pendingMigrations = getAllMigrations().filter { it.version !in appliedMigrations }

        if (pendingMigrations.isEmpty()) {
            logger.info("No pending migrations")
            return
        }

        logger.info("Found ${pendingMigrations.size} pending migration(s)")

        pendingMigrations.forEach { migration ->
            applyMigration(migration)
        }

        logger.info("All migrations applied successfully")
    }

    /**
     * All migrations defined in order.
     * Add new migrations to this list.
     */
    private fun getAllMigrations(): List<Migration> = listOf(
        Migration(
            version = "V001",
            description = "Add finished_at to rounds",
            apply = {
                val isPostgres = db.vendor == "postgresql"
                val isH2 = db.vendor == "h2"

                // Check if column already exists
                val columnExists = try {
                    exec("SELECT finished_at FROM rounds LIMIT 1")
                    true
                } catch (e: Exception) {
                    false
                }

                if (columnExists) {
                    logger.info("Column finished_at already exists, skipping ALTER TABLE")
                } else {
                    when {
                        isPostgres -> exec("ALTER TABLE rounds ADD COLUMN finished_at TIMESTAMP NULL")
                        isH2 -> exec("ALTER TABLE rounds ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP NULL")
                        else -> exec("ALTER TABLE rounds ADD COLUMN finished_at TIMESTAMP NULL")
                    }
                }

                // Backfill existing finished rounds
                exec("UPDATE rounds SET finished_at = updated_at WHERE finished = TRUE AND finished_at IS NULL")
            }
        )
    )

    private fun getAppliedMigrations(): Set<String> = transaction {
        MigrationTable.selectAll().map { it[MigrationTable.version] }.toSet()
    }

    private fun applyMigration(migration: Migration) {
        logger.info("Applying migration ${migration.version}: ${migration.description}")

        try {
            transaction {
                migration.apply(this)
            }

            // Record migration as applied
            transaction {
                MigrationTable.insert {
                    it[version] = migration.version
                    it[description] = migration.description
                    it[appliedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                }
            }

            logger.info("Migration ${migration.version} applied successfully")
        } catch (e: Exception) {
            logger.error("Migration ${migration.version} failed: ${e.message}", e)
            throw e
        }
    }

    private data class Migration(
        val version: String,
        val description: String,
        val apply: Transaction.() -> Unit
    )
}
