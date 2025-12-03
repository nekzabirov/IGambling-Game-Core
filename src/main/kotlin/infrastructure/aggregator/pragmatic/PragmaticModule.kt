package infrastructure.aggregator.pragmatic

import infrastructure.aggregator.pragmatic.adapter.PragmaticCurrencyAdapter
import org.koin.dsl.module

internal val PragmaticModule = module {
    single { PragmaticCurrencyAdapter(get()) }

    factory { PragmaticAdapterFactory(get()) }
}
