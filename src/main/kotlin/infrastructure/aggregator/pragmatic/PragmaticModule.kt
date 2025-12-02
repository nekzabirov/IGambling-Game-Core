package infrastructure.aggregator.pragmatic

import infrastructure.aggregator.pragmatic.adapter.PragmaticCurrencyAdapter
import infrastructure.aggregator.pragmatic.handler.PragmaticHandler
import org.koin.dsl.module

internal val PragmaticModule = module {
    single { PragmaticCurrencyAdapter(get()) }

    factory { PragmaticHandler(get(), get(), get(), get(), get()) }

    factory { PragmaticAdapterFactory(get()) }
}
