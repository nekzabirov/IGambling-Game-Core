package com.nekgamebling.infrastructure.api

import com.nekgamebling.infrastructure.api.grpc.service.*
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.server.application.*

fun Application.installApi() {
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

    server.awaitTermination()
}