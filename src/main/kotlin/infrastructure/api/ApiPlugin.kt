package com.nekgamebling.infrastructure.api

import com.nekgamebling.infrastructure.api.rest.aggregatorRoute
import infrastructure.api.grpc.service.*
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory

private val REQUEST_START_TIME = AttributeKey<Long>("RequestStartTime")
private val RESPONSE_BODY = AttributeKey<String>("ResponseBody")
private val logger = LoggerFactory.getLogger("HttpRequestLogger")

val RequestLoggingPlugin = createApplicationPlugin(name = "RequestLoggingPlugin") {
    onCall { call ->
        call.attributes.put(REQUEST_START_TIME, System.currentTimeMillis())
    }

    onCallRespond { call, body ->
        val responseBody = when (body) {
            is String -> body
            else -> body.toString()
        }
        call.attributes.put(RESPONSE_BODY, responseBody.take(2000))
    }

    onCallRespond { call, _ ->
        val duration = call.attributes.getOrNull(REQUEST_START_TIME)?.let {
            System.currentTimeMillis() - it
        } ?: 0

        val httpMethod = call.request.httpMethod.value
        val path = call.request.path()
        val queryParams = call.request.queryString()
        val status = call.response.status()

        val headers = call.request.headers.toMap()
            .filterKeys { it.lowercase() !in listOf("authorization", "cookie") }
            .map { (k, v) -> "$k: ${v.joinToString()}" }
            .joinToString(", ")

        val responseBody = call.attributes.getOrNull(RESPONSE_BODY) ?: ""

        val logMessage = buildString {
            appendLine()
            appendLine("┌──────────────────────────────────────────────────────────────")
            appendLine("│ $httpMethod $path${if (queryParams.isNotEmpty()) "?$queryParams" else ""}")
            appendLine("│ Status: $status | Duration: ${duration}ms")
            appendLine("│ Headers: $headers")
            appendLine("│ Response: $responseBody")
            append("└──────────────────────────────────────────────────────────────")
        }

        logger.info(logMessage)
    }
}

fun Application.installApi() {
    install(RequestLoggingPlugin)

    installRest()
    installGrpc()
}

private fun Application.installGrpc() {
    val gameService: GameGrpcService by getKoin().inject()
    val providerService: ProviderGrpcService by getKoin().inject()
    val collectionService: CollectionGrpcService by getKoin().inject()
    val roundService: RoundGrpcService by getKoin().inject()
    val aggregatorService: AggregatorGrpcService by getKoin().inject()

    val server: Server = NettyServerBuilder
        .forPort(5050)
        .maxInboundMessageSize(50 * 1024 * 1024) // 50 MB
        .addService(gameService)
        .addService(providerService)
        .addService(collectionService)
        .addService(roundService)
        .addService(aggregatorService)
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