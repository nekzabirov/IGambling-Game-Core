package infrastructure.aggregator.onegamehub

import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.handler.OneGameHubHandler
import org.koin.dsl.module

internal val OneGameHubModule = module {
    single { OneGameHubCurrencyAdapter(get()) }

    factory { OneGameHubHandler(get(), get(), get(), get(), get(), get(), get()) }

    factory { OneGameHubAdapterFactory(get()) }
}