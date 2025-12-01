package infrastructure.api

import infrastructure.api.grpc.service.*
import infrastructure.api.rest.aggregatorRoute
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.event.Level

fun Application.installApi() {
    install(CallLogging) {
        level = Level.INFO
    }

    installRest()
    installGrpc()
}

private fun Application.installGrpc() {
    val server: Server = NettyServerBuilder
        .forPort(5050)
        .addService(SyncServiceImpl(this))
        .addService(CollectionServiceImpl(this))
        .addService(ProviderServiceImpl(this))
        .addService(GameServiceImpl(this))
        .addService(SessionServiceImpl(this))
        .addService(FreespinServiceImpl(this))
        .build()
        .start()

    launch(Dispatchers.IO) {
        server.awaitTermination()
    }
}

private fun Application.installRest() {
    routing {
        route("webhook") {
            aggregatorRoute()
        }
    }
}