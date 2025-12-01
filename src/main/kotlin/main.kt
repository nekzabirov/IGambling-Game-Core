package com.nekgamebling

import com.nekgamebling.config.DatabaseConfig
import com.nekgamebling.config.coreModule
import com.nekgamebling.infrastructure.api.installApi
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    embeddedServer(
        CIO,
        port = 80,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    DatabaseConfig.init()

    install(Koin) {
        slf4jLogger()
        modules(coreModule())
    }

    install(ContentNegotiation) {
        json()
    }

    installApi()
}
