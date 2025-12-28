package infrastructure.persistence

import application.port.outbound.CacheAdapter
import application.port.outbound.GameSyncAdapter
import domain.aggregator.repository.AggregatorRepository
import domain.collection.repository.CollectionRepository
import domain.game.repository.GameFavouriteRepository
import domain.game.repository.GameRepository
import domain.game.repository.GameVariantRepository
import domain.game.repository.GameWonRepository
import domain.provider.repository.ProviderRepository
import domain.session.repository.RoundRepository
import domain.session.repository.SessionRepository
import domain.session.repository.SpinRepository
import infrastructure.persistence.cache.InMemoryCacheAdapter
import infrastructure.persistence.exposed.adapter.ExposedGameSyncAdapter
import infrastructure.persistence.exposed.repository.ExposedAggregatorRepository
import infrastructure.persistence.exposed.repository.ExposedCollectionRepository
import infrastructure.persistence.exposed.repository.ExposedGameFavouriteRepository
import infrastructure.persistence.exposed.repository.ExposedGameRepository
import infrastructure.persistence.exposed.repository.ExposedGameVariantRepository
import infrastructure.persistence.exposed.repository.ExposedGameWonRepository
import infrastructure.persistence.exposed.repository.ExposedProviderRepository
import infrastructure.persistence.exposed.repository.ExposedRoundRepository
import infrastructure.persistence.exposed.repository.ExposedSessionRepository
import infrastructure.persistence.exposed.repository.ExposedSpinRepository
import infrastructure.persistence.exposed.repository.ExposedGameQueryRepository
import application.query.game.GameQueryRepository
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Database configuration from environment variables.
 */
data class DatabaseEnvConfig(
    val jdbcUrl: String = System.getenv("DB_URL") ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
    val username: String = System.getenv("DB_USER") ?: "",
    val password: String = System.getenv("DB_PASSWORD") ?: "",
    val poolProfile: String = System.getenv("DB_POOL_PROFILE") ?: "development"
)

val DBModule = module {
    databaseModule()
    repositoryModule()
    cacheModule()
}

/**
 * Configure database with HikariCP connection pool.
 */
private fun Module.databaseModule() {
    single<Database> {
        val envConfig = DatabaseEnvConfig()
        val poolConfig = when (envConfig.poolProfile.lowercase()) {
            "production" -> DatabasePoolConfig.production()
            "high-throughput" -> DatabasePoolConfig.highThroughput()
            else -> DatabasePoolConfig.development()
        }
        DatabaseConfig.configure(
            jdbcUrl = envConfig.jdbcUrl,
            username = envConfig.username,
            password = envConfig.password,
            config = poolConfig
        )
    }
}

private fun Module.repositoryModule() {
    // ==========================================
    // Infrastructure - Repositories (Write)
    // ==========================================
    single<GameRepository> { ExposedGameRepository() }
    single<GameVariantRepository> { ExposedGameVariantRepository() }
    single<GameFavouriteRepository> { ExposedGameFavouriteRepository() }
    single<GameWonRepository> { ExposedGameWonRepository() }
    single<SessionRepository> { ExposedSessionRepository() }
    single<RoundRepository> { ExposedRoundRepository() }
    single<SpinRepository> { ExposedSpinRepository() }
    single<ProviderRepository> { ExposedProviderRepository() }
    single<CollectionRepository> { ExposedCollectionRepository() }
    single<AggregatorRepository> { ExposedAggregatorRepository() }

    // ==========================================
    // Infrastructure - Query Repositories (Read)
    // ==========================================
    single<GameQueryRepository> { ExposedGameQueryRepository() }

    single<GameSyncAdapter> { ExposedGameSyncAdapter() }
}

private fun Module.cacheModule() {
    single<CacheAdapter> { InMemoryCacheAdapter() }
}
