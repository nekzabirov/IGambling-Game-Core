package infrastructure.aggregator.pateplay

import infrastructure.aggregator.pateplay.adapter.PateplayCurrencyAdapter
import org.koin.dsl.module

internal val PateplayModule = module {
    single { PateplayCurrencyAdapter(get()) }

    factory { PateplayAdapterFactory(get()) }
}
