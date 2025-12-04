package infrastructure.aggregator.onegamehub

import com.nekgamebling.infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.handler.OneGameHubHandler
import org.koin.dsl.module

internal val OneGameHubModule = module {
    single { OneGameHubCurrencyAdapter(get()) }

    factory { OneGameHubHandler(get(), get(), get(), get(), get(), get()) }

    factory { OneGameHubAdapterFactory(get()) }
}