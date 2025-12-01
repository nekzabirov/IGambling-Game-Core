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
import org.koin.core.module.Module
import org.koin.dsl.module

val DBModule = module {
    repositoryModule()
    cacheModule()
}

private fun Module.repositoryModule() {
    // ==========================================
    // Infrastructure - Repositories
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

    single<GameSyncAdapter> { ExposedGameSyncAdapter() }
}

private fun Module.cacheModule() {
    single<CacheAdapter> { InMemoryCacheAdapter() }
}
