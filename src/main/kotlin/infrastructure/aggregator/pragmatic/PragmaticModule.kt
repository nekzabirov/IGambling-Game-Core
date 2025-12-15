package infrastructure.aggregator.pragmatic

import com.nekgamebling.infrastructure.aggregator.pragmatic.handler.PragmaticHandler
import org.koin.dsl.module

internal val PragmaticModule = module {
    factory { PragmaticHandler(get(), get(), get(), get(), get(), get(), get(), get()) }

    factory { PragmaticAdapterFactory(get()) }
}
