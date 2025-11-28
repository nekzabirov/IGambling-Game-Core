import com.nekgamebling.application.usecase.aggregator.ListAllActiveAggregatorUsecase
import com.nekgamebling.application.usecase.aggregator.SyncGameUsecase
import com.nekgamebling.config.coreModule
import com.nekgamebling.installCore
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val logger by lazy {
    LoggerFactory.getLogger("SyncGamesJob")
}

fun main() = runBlocking {
    // Set timezone before any database operations
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    val server = embeddedServer(CIO, port = 0) {
        installCore()
    }

    logger.info("Start job application")

    server.start(wait = false)

    // Give the server/DI a moment to fully settle if there are async initializers
    delay(1000)

    val koin = server.application.getKoin()
    val syncGameUsecase = koin.get<SyncGameUsecase>()
    val listAllActiveAggregatorUsecase = koin.get<ListAllActiveAggregatorUsecase>()

    val aggregators = listAllActiveAggregatorUsecase()

    for (aggregatorInfo in aggregators) {
        logger.info("Start sync game aggregator ${aggregatorInfo.identity}")

        val result = syncGameUsecase(aggregatorInfo.identity)

        if (result.isSuccess) {
            logger.info("${aggregatorInfo.identity} synced ${result.getOrThrow().gameCount} games.")
        } else {
            logger.info("${aggregatorInfo.identity} not synced ${result.exceptionOrNull()?.message}")
        }
    }

    logger.info("Syncing finished")

    server.stop()
    exitProcess(0)

    Unit
}