package infrastructure.aggregator

import application.port.outbound.AggregatorAdapterRegistry
import infrastructure.aggregator.onegamehub.OneGameHubModule
import infrastructure.aggregator.pragmatic.PragmaticModule
import org.koin.dsl.module

internal val AggregatorModule = module {
    includes(OneGameHubModule)
    includes(PragmaticModule)

    factory<AggregatorAdapterRegistry> {
        AggregatorAdapterRegistryImpl(get(), get())
    }
}