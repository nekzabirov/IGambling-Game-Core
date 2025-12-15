package infrastructure.aggregator.pateplay

import org.koin.dsl.module

internal val PateplayModule = module {
    factory { PateplayAdapterFactory(get()) }
}
