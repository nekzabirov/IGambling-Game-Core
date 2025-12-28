package infrastructure.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

/**
 * Database configuration with HikariCP connection pooling.
 *
 * Provides optimized connection pooling for production workloads:
 * - Connection reuse reduces connection overhead
 * - Statement caching improves query performance
 * - Proper timeouts prevent resource leaks
 */
object DatabaseConfig {

    /**
     * Configure database with HikariCP connection pool.
     *
     * @param jdbcUrl JDBC connection URL
     * @param username Database username
     * @param password Database password
     * @param config Optional custom configuration
     * @return Configured Database instance
     */
    fun configure(
        jdbcUrl: String,
        username: String = "",
        password: String = "",
        config: DatabasePoolConfig = DatabasePoolConfig()
    ): Database {
        val dataSource = createDataSource(jdbcUrl, username, password, config)
        return Database.connect(dataSource)
    }

    /**
     * Create HikariCP data source.
     */
    fun createDataSource(
        jdbcUrl: String,
        username: String = "",
        password: String = "",
        config: DatabasePoolConfig = DatabasePoolConfig()
    ): DataSource {
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            if (username.isNotBlank()) {
                this.username = username
            }
            if (password.isNotBlank()) {
                this.password = password
            }

            // Pool sizing
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdle

            // Connection timeouts
            connectionTimeout = config.connectionTimeout
            idleTimeout = config.idleTimeout
            maxLifetime = config.maxLifetime

            // Validation
            validationTimeout = config.validationTimeout

            // Pool name for metrics/logging
            poolName = config.poolName

            // Performance optimizations for PostgreSQL
            if (jdbcUrl.contains("postgresql", ignoreCase = true)) {
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("rewriteBatchedStatements", "true")
            }

            // Auto-commit for Exposed (it manages transactions)
            isAutoCommit = false

            // Transaction isolation
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        }

        return HikariDataSource(hikariConfig)
    }
}

/**
 * Database pool configuration options.
 */
data class DatabasePoolConfig(
    /** Maximum number of connections in the pool */
    val maxPoolSize: Int = 10,

    /** Minimum number of idle connections to maintain */
    val minIdle: Int = 5,

    /** Maximum time to wait for a connection (ms) */
    val connectionTimeout: Long = 10_000,

    /** Maximum time a connection can be idle (ms) */
    val idleTimeout: Long = 300_000, // 5 minutes

    /** Maximum lifetime of a connection (ms) */
    val maxLifetime: Long = 600_000, // 10 minutes

    /** Timeout for connection validation (ms) */
    val validationTimeout: Long = 5_000,

    /** Pool name for metrics/logging */
    val poolName: String = "GameCorePool"
) {
    companion object {
        /**
         * Default configuration for production.
         */
        fun production() = DatabasePoolConfig(
            maxPoolSize = 20,
            minIdle = 5,
            connectionTimeout = 30_000,
            idleTimeout = 600_000,
            maxLifetime = 1_800_000 // 30 minutes
        )

        /**
         * Configuration for development/testing.
         */
        fun development() = DatabasePoolConfig(
            maxPoolSize = 5,
            minIdle = 2,
            connectionTimeout = 10_000,
            idleTimeout = 60_000, // 1 minute
            maxLifetime = 300_000 // 5 minutes
        )

        /**
         * Configuration for high-throughput environments.
         */
        fun highThroughput() = DatabasePoolConfig(
            maxPoolSize = 50,
            minIdle = 10,
            connectionTimeout = 5_000,
            idleTimeout = 300_000,
            maxLifetime = 900_000 // 15 minutes
        )
    }
}
