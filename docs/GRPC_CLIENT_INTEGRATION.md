# Game Core gRPC Client Integration Guide

This guide demonstrates how to integrate the Game Core gRPC client into your application using Spring Boot with Kotlin.

## Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Available Services](#available-services)
- [Usage Examples](#usage-examples)
  - [Session Service](#session-service)
  - [Game Service](#game-service)
  - [Provider Service](#provider-service)
  - [Collection Service](#collection-service)
  - [Freespin Service](#freespin-service)
  - [Sync Service](#sync-service)
  - [Round Service](#round-service)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)

## Installation

### Gradle (Kotlin DSL)

Add the GitHub Packages repository and dependency to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/nekzabirov/igaming-aggregator-core.git")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
        }
    }
}

dependencies {
    // Game Core gRPC Client
    implementation("com.nekgamebling:game-core-grpc-client:1.0.0")

    // gRPC Runtime (required)
    implementation("io.grpc:grpc-netty-shaded:1.68.2")
    implementation("io.grpc:grpc-stub:1.68.2")
    implementation("io.grpc:grpc-protobuf:1.68.2")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")

    // Protobuf
    implementation("com.google.protobuf:protobuf-kotlin:4.29.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/nekzabirov/igaming-aggregator-core.git")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user")
            password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.token")
        }
    }
}

dependencies {
    implementation 'com.nekgamebling:game-core-grpc-client:1.0.0'
    implementation 'io.grpc:grpc-netty-shaded:1.68.2'
    implementation 'io.grpc:grpc-stub:1.68.2'
    implementation 'io.grpc:grpc-protobuf:1.68.2'
    implementation 'io.grpc:grpc-kotlin-stub:1.4.1'
    implementation 'com.google.protobuf:protobuf-kotlin:4.29.2'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0'
}
```

## Configuration

### Spring Boot Configuration

Create a configuration class for the gRPC channel:

```kotlin
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.PreDestroy

@Configuration
class GrpcConfig {

    @Value("\${game-core.grpc.host:localhost}")
    private lateinit var host: String

    @Value("\${game-core.grpc.port:5050}")
    private var port: Int = 5050

    private var channel: ManagedChannel? = null

    @Bean
    fun gameCoreChannel(): ManagedChannel {
        channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext() // Use .useTransportSecurity() for TLS
            .maxInboundMessageSize(50 * 1024 * 1024) // 50 MB
            .build()
        return channel!!
    }

    @PreDestroy
    fun shutdown() {
        channel?.shutdown()
    }
}
```

### Application Properties

```yaml
# application.yml
game-core:
  grpc:
    host: localhost
    port: 5050
```

### Service Beans Configuration

```kotlin
import com.nekzabirov.igambling.proto.service.*
import io.grpc.ManagedChannel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameCoreServiceConfig {

    @Bean
    fun sessionServiceStub(channel: ManagedChannel): SessionGrpcKt.SessionCoroutineStub {
        return SessionGrpcKt.SessionCoroutineStub(channel)
    }

    @Bean
    fun gameServiceStub(channel: ManagedChannel): GameGrpcKt.GameCoroutineStub {
        return GameGrpcKt.GameCoroutineStub(channel)
    }

    @Bean
    fun providerServiceStub(channel: ManagedChannel): ProviderGrpcKt.ProviderCoroutineStub {
        return ProviderGrpcKt.ProviderCoroutineStub(channel)
    }

    @Bean
    fun collectionServiceStub(channel: ManagedChannel): CollectionGrpcKt.CollectionCoroutineStub {
        return CollectionGrpcKt.CollectionCoroutineStub(channel)
    }

    @Bean
    fun freespinServiceStub(channel: ManagedChannel): FreespinGrpcKt.FreespinCoroutineStub {
        return FreespinGrpcKt.FreespinCoroutineStub(channel)
    }

    @Bean
    fun syncServiceStub(channel: ManagedChannel): SyncGrpcKt.SyncCoroutineStub {
        return SyncGrpcKt.SyncCoroutineStub(channel)
    }

    @Bean
    fun roundServiceStub(channel: ManagedChannel): RoundGrpcKt.RoundCoroutineStub {
        return RoundGrpcKt.RoundCoroutineStub(channel)
    }
}
```

## Available Services

| Service | Description |
|---------|-------------|
| `Session` | Open game sessions for players |
| `Game` | List, update, and manage games |
| `Provider` | List and configure game providers |
| `Collection` | Manage game collections/categories |
| `Freespin` | Create and manage free spins |
| `Sync` | Aggregator and variant synchronization |
| `Round` | Query round details with aggregated amounts |

## Usage Examples

### Session Service

Open a game session for a player:

```kotlin
import com.nekzabirov.igambling.proto.dto.PlatformType
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service

@Service
class GameSessionService(
    private val sessionStub: SessionGrpcKt.SessionCoroutineStub
) {

    suspend fun openSession(
        gameIdentity: String,
        playerId: String,
        currency: String,
        locale: String,
        platform: PlatformType,
        lobbyUrl: String
    ): String {
        val command = openSessionCommand {
            this.gameIdentity = gameIdentity
            this.playerId = playerId
            this.currency = currency
            this.locale = locale
            this.platform = platform
            this.lobbyUrl = lobbyUrl
        }

        val result = sessionStub.openSession(command)
        return result.launchUrl
    }
}
```

**REST Controller Example:**

```kotlin
import com.nekzabirov.igambling.proto.dto.PlatformType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sessions")
class SessionController(
    private val gameSessionService: GameSessionService
) {

    data class OpenSessionRequest(
        val gameIdentity: String,
        val playerId: String,
        val currency: String,
        val locale: String,
        val platform: String,
        val lobbyUrl: String
    )

    data class OpenSessionResponse(val launchUrl: String)

    @PostMapping("/open")
    suspend fun openSession(@RequestBody request: OpenSessionRequest): OpenSessionResponse {
        val platform = when (request.platform.uppercase()) {
            "DESKTOP" -> PlatformType.DESKTOP
            "MOBILE" -> PlatformType.MOBILE
            else -> PlatformType.UNRECOGNIZED
        }

        val launchUrl = gameSessionService.openSession(
            gameIdentity = request.gameIdentity,
            playerId = request.playerId,
            currency = request.currency,
            locale = request.locale,
            platform = platform,
            lobbyUrl = request.lobbyUrl
        )

        return OpenSessionResponse(launchUrl)
    }
}
```

### Game Service

Find, list, and manage games:

```kotlin
import com.nekzabirov.igambling.proto.dto.PlatformType
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service

@Service
class GameService(
    private val gameStub: GameGrpcKt.GameCoroutineStub
) {

    /**
     * Find a game by identity with full details including provider and aggregator info.
     */
    suspend fun findGame(identity: String): FindGameResult {
        val command = findGameCommand {
            this.identity = identity
        }

        return gameStub.find(command)
    }

    suspend fun listGames(
        query: String = "",
        active: Boolean? = null,
        pageNumber: Int = 0,
        pageSize: Int = 20,
        providerIdentities: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        platforms: List<PlatformType> = emptyList()
    ): ListGameResult {
        val command = listGameCommand {
            this.query = query
            active?.let { this.active = it }
            this.pageNumber = pageNumber
            this.pageSize = pageSize
            this.providerIdentity.addAll(providerIdentities)
            this.tags.addAll(tags)
            this.platforms.addAll(platforms)
        }

        return gameStub.list(command)
    }

    suspend fun updateGame(
        identity: String,
        active: Boolean,
        bonusBet: Boolean,
        bonusWagering: Boolean
    ) {
        val command = updateGameConfig {
            this.identity = identity
            this.active = active
            this.bonusBet = bonusBet
            this.bonusWagering = bonusWagering
        }

        gameStub.update(command)
    }

    suspend fun addTag(gameIdentity: String, tag: String) {
        val command = gameTagCommand {
            this.identity = gameIdentity
            this.tag = tag
        }
        gameStub.addTag(command)
    }

    suspend fun removeTag(gameIdentity: String, tag: String) {
        val command = gameTagCommand {
            this.identity = gameIdentity
            this.tag = tag
        }
        gameStub.removeTag(command)
    }

    suspend fun addFavourite(gameIdentity: String, playerId: String) {
        val command = gameFavouriteCommand {
            this.gameIdentity = gameIdentity
            this.playerId = playerId
        }
        gameStub.addFavourite(command)
    }

    suspend fun removeFavourite(gameIdentity: String, playerId: String) {
        val command = gameFavouriteCommand {
            this.gameIdentity = gameIdentity
            this.playerId = playerId
        }
        gameStub.removeFavourite(command)
    }

    suspend fun getDemoGame(
        gameIdentity: String,
        currency: String,
        locale: String,
        platform: PlatformType,
        lobbyUrl: String
    ): String {
        val command = demoGameCommand {
            this.gameIdentity = gameIdentity
            this.currency = currency
            this.locale = locale
            this.platform = platform
            this.lobbyUrl = lobbyUrl
        }

        return gameStub.demoGame(command).launchUrl
    }

    /**
     * Upload or update a game image.
     * @param identity Game identifier
     * @param key Image key (e.g., "thumbnail", "banner", "logo")
     * @param ext File extension (e.g., "png", "jpg", "webp")
     * @param data Image binary data
     */
    suspend fun updateImage(
        identity: String,
        key: String,
        ext: String,
        data: ByteArray
    ) {
        val command = updateGameImageCommand {
            this.identity = identity
            this.key = key
            this.ext = ext
            this.data = com.google.protobuf.ByteString.copyFrom(data)
        }
        gameStub.updateImage(command)
    }
}
```

### Provider Service

List and configure providers:

```kotlin
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service

@Service
class ProviderService(
    private val providerStub: ProviderGrpcKt.ProviderCoroutineStub
) {

    suspend fun listProviders(
        query: String = "",
        active: Boolean? = null,
        pageNumber: Int = 0,
        pageSize: Int = 20
    ): ListProviderResult {
        val command = listProviderCommand {
            this.query = query
            active?.let { this.active = it }
            this.pageNumber = pageNumber
            this.pageSize = pageSize
        }

        return providerStub.list(command)
    }

    suspend fun updateProvider(
        identity: String,
        order: Int,
        active: Boolean
    ) {
        val command = updateProviderConfig {
            this.identity = identity
            this.order = order
            this.active = active
        }

        providerStub.update(command)
    }

    /**
     * Upload or update a provider image.
     * @param identity Provider identifier
     * @param key Image key (e.g., "logo", "banner")
     * @param ext File extension (e.g., "png", "jpg", "webp")
     * @param data Image binary data
     */
    suspend fun updateImage(
        identity: String,
        key: String,
        ext: String,
        data: ByteArray
    ) {
        val command = updateProviderImageCommand {
            this.identity = identity
            this.key = key
            this.ext = ext
            this.data = com.google.protobuf.ByteString.copyFrom(data)
        }
        providerStub.updateImage(command)
    }
}
```

### Collection Service

Manage game collections:

```kotlin
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service

@Service
class CollectionService(
    private val collectionStub: CollectionGrpcKt.CollectionCoroutineStub
) {

    suspend fun createCollection(
        identity: String,
        names: Map<String, String>  // locale -> name
    ) {
        val command = addCollectionCommand {
            this.identity = identity
            this.name.putAll(names)
        }

        collectionStub.addCollection(command)
    }

    suspend fun updateCollection(
        identity: String,
        names: Map<String, String>,
        order: Int,
        active: Boolean
    ) {
        val command = updateCollectionCommand {
            this.identity = identity
            this.name.putAll(names)
            this.order = order
            this.active = active
        }

        collectionStub.updateCollection(command)
    }

    suspend fun addGameToCollection(collectionIdentity: String, gameIdentity: String) {
        val command = addGameCollectionCommand {
            this.identity = collectionIdentity
            this.gameIdentity = gameIdentity
        }

        collectionStub.addGameCollection(command)
    }

    suspend fun removeGameFromCollection(collectionIdentity: String, gameIdentity: String) {
        val command = addGameCollectionCommand {
            this.identity = collectionIdentity
            this.gameIdentity = gameIdentity
        }

        collectionStub.removeGameFromCollection(command)
    }

    suspend fun changeGameOrder(
        collectionIdentity: String,
        gameIdentity: String,
        order: Int
    ) {
        val command = changeGameOrderCollectionCommand {
            this.identity = collectionIdentity
            this.gameIdentity = gameIdentity
            this.order = order
        }

        collectionStub.changeGameOrder(command)
    }

    suspend fun listCollections(
        query: String = "",
        active: Boolean? = null,
        pageNumber: Int = 0,
        pageSize: Int = 20
    ): ListCollectionResult {
        val command = listCollectionCommand {
            this.query = query
            active?.let { this.active = it }
            this.pageNumber = pageNumber
            this.pageSize = pageSize
        }

        return collectionStub.list(command)
    }
}
```

### Freespin Service

Create and manage free spins:

```kotlin
import com.google.protobuf.Timestamp
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class FreespinService(
    private val freespinStub: FreespinGrpcKt.FreespinCoroutineStub
) {

    suspend fun getPreset(gameIdentity: String): GetPresetResult {
        val command = getPresetCommand {
            this.gameIdentity = gameIdentity
        }

        return freespinStub.getPreset(command)
    }

    suspend fun createFreespin(
        referenceId: String,
        playerId: String,
        gameIdentity: String,
        currency: String,
        presetValues: Map<String, Int>,
        startAt: Instant,
        endAt: Instant
    ) {
        val command = createFreespinCommand {
            this.referenceId = referenceId
            this.playerId = playerId
            this.gameIdentity = gameIdentity
            this.currency = currency
            this.presetValue.putAll(presetValues)
            this.startAt = Timestamp.newBuilder()
                .setSeconds(startAt.epochSecond)
                .setNanos(startAt.nano)
                .build()
            this.endAt = Timestamp.newBuilder()
                .setSeconds(endAt.epochSecond)
                .setNanos(endAt.nano)
                .build()
        }

        freespinStub.createFreespin(command)
    }

    suspend fun cancelFreespin(referenceId: String, gameIdentity: String) {
        val command = cancelFreespinCommand {
            this.referenceId = referenceId
            this.gameIdentity = gameIdentity
        }

        freespinStub.cancelFreespin(command)
    }
}
```

### Sync Service

Aggregator and variant synchronization:

```kotlin
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service

@Service
class SyncService(
    private val syncStub: SyncGrpcKt.SyncCoroutineStub
) {

    suspend fun addAggregator(
        identity: String,
        type: String,
        config: Map<String, String>
    ) {
        val command = addAggregatorCommand {
            this.identity = identity
            this.type = type
            this.config.putAll(config)
        }

        syncStub.addAggregator(command)
    }

    suspend fun listAggregators(
        query: String = "",
        type: String? = null,
        active: Boolean? = null,
        pageNumber: Int = 0,
        pageSize: Int = 20
    ): ListAggregatorResult {
        val command = listAggregatorCommand {
            this.query = query
            type?.let { this.type = it }
            active?.let { this.active = it }
            this.pageNumber = pageNumber
            this.pageSize = pageSize
        }

        return syncStub.listAggregator(command)
    }

    suspend fun assignGameVariant(variantId: String, gameId: String) {
        val command = assignGameVariantCommand {
            this.variantId = variantId
            this.gameId = gameId
        }

        syncStub.assignGameVariant(command)
    }

    suspend fun assignProvider(
        providerId: String,
        aggregatorIdentity: String,
        partnerType: Int
    ) {
        val command = assignProviderCommand {
            this.providerId = providerId
            this.aggregatorIdentity = aggregatorIdentity
            this.partnerType = partnerType
        }

        syncStub.assignProvider(command)
    }

    suspend fun listVariants(
        query: String = "",
        aggregatorType: String? = null,
        gameIdentity: String? = null,
        pageNumber: Int = 0,
        pageSize: Int = 20
    ): ListVariantResult {
        val command = listVariantsCommand {
            this.query = query
            aggregatorType?.let { this.aggregatorType = it }
            gameIdentity?.let { this.gameIdentity = it }
            this.pageNumber = pageNumber
            this.pageSize = pageSize
        }

        return syncStub.listVariants(command)
    }
}
```

### Round Service

Query round details with aggregated spin amounts:

```kotlin
import com.nekzabirov.igambling.proto.service.*
import org.springframework.stereotype.Service

@Service
class RoundService(
    private val roundStub: RoundGrpcKt.RoundCoroutineStub
) {

    /**
     * Get paginated round details with aggregated amounts and game info.
     * @param playerId Optional filter by player ID
     * @param gameIdentity Optional filter by game identity
     * @param pageNumber Page number (1-based)
     * @param pageSize Number of items per page
     */
    suspend fun getRoundsDetails(
        playerId: String? = null,
        gameIdentity: String? = null,
        pageNumber: Int = 1,
        pageSize: Int = 20
    ): GetRoundsDetailsResult {
        val command = getRoundsDetailsCommand {
            this.pageNumber = pageNumber
            this.pageSize = pageSize
            playerId?.let { this.playerId = it }
            gameIdentity?.let { this.gameIdentity = it }
        }

        return roundStub.getRoundsDetails(command)
    }

    /**
     * Get all rounds for a specific player.
     */
    suspend fun getPlayerRounds(
        playerId: String,
        pageNumber: Int = 1,
        pageSize: Int = 20
    ): GetRoundsDetailsResult {
        return getRoundsDetails(
            playerId = playerId,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }

    /**
     * Get all rounds for a specific game.
     */
    suspend fun getGameRounds(
        gameIdentity: String,
        pageNumber: Int = 1,
        pageSize: Int = 20
    ): GetRoundsDetailsResult {
        return getRoundsDetails(
            gameIdentity = gameIdentity,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }
}
```

**REST Controller Example:**

```kotlin
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/rounds")
class RoundController(
    private val roundService: RoundService
) {

    data class RoundDetailResponse(
        val id: String,
        val placeAmount: String,
        val settleAmount: String,
        val freeSpinId: String?,
        val currency: String,
        val gameName: String,
        val gameIdentity: String,
        val isFinished: Boolean
    )

    data class PagedResponse<T>(
        val items: List<T>,
        val totalPages: Int,
        val totalItems: Long,
        val currentPage: Int
    )

    @GetMapping
    suspend fun getRounds(
        @RequestParam(required = false) playerId: String?,
        @RequestParam(required = false) gameIdentity: String?,
        @RequestParam(defaultValue = "1") pageNumber: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): PagedResponse<RoundDetailResponse> {
        val result = roundService.getRoundsDetails(
            playerId = playerId,
            gameIdentity = gameIdentity,
            pageNumber = pageNumber,
            pageSize = pageSize
        )

        return PagedResponse(
            items = result.itemsList.map { round ->
                RoundDetailResponse(
                    id = round.id,
                    placeAmount = round.placeAmount,
                    settleAmount = round.settleAmount,
                    freeSpinId = if (round.hasFreeSpinId()) round.freeSpinId else null,
                    currency = round.currency,
                    gameName = round.game.name,
                    gameIdentity = round.game.identity,
                    isFinished = round.isFinished
                )
            },
            totalPages = result.totalPage,
            totalItems = result.totalItems,
            currentPage = result.currentPage
        )
    }

    @GetMapping("/player/{playerId}")
    suspend fun getPlayerRounds(
        @PathVariable playerId: String,
        @RequestParam(defaultValue = "1") pageNumber: Int,
        @RequestParam(defaultValue = "20") pageSize: Int
    ): PagedResponse<RoundDetailResponse> {
        return getRounds(
            playerId = playerId,
            gameIdentity = null,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }
}
```

## Error Handling

Implement proper error handling for gRPC calls:

```kotlin
import io.grpc.Status
import io.grpc.StatusException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GrpcErrorHandler {

    private val logger = LoggerFactory.getLogger(GrpcErrorHandler::class.java)

    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: StatusException) {
            logger.error("gRPC error: ${e.status.code} - ${e.status.description}", e)

            when (e.status.code) {
                Status.Code.NOT_FOUND -> Result.failure(NotFoundException(e.status.description ?: "Resource not found"))
                Status.Code.INVALID_ARGUMENT -> Result.failure(BadRequestException(e.status.description ?: "Invalid argument"))
                Status.Code.UNAVAILABLE -> Result.failure(ServiceUnavailableException("Game Core service is unavailable"))
                Status.Code.DEADLINE_EXCEEDED -> Result.failure(TimeoutException("Request timed out"))
                else -> Result.failure(e)
            }
        }
    }
}

class NotFoundException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : RuntimeException(message)
class ServiceUnavailableException(message: String) : RuntimeException(message)
class TimeoutException(message: String) : RuntimeException(message)
```

**Usage with error handling:**

```kotlin
@Service
class SafeGameSessionService(
    private val sessionStub: SessionGrpcKt.SessionCoroutineStub,
    private val errorHandler: GrpcErrorHandler
) {

    suspend fun openSession(
        gameIdentity: String,
        playerId: String,
        currency: String,
        locale: String,
        platform: PlatformType,
        lobbyUrl: String
    ): Result<String> {
        return errorHandler.execute {
            val command = openSessionCommand {
                this.gameIdentity = gameIdentity
                this.playerId = playerId
                this.currency = currency
                this.locale = locale
                this.platform = platform
                this.lobbyUrl = lobbyUrl
            }

            sessionStub.openSession(command).launchUrl
        }
    }
}
```

## Best Practices

### 1. Connection Management

Use connection pooling and keep-alive for production:

```kotlin
@Bean
fun gameCoreChannel(): ManagedChannel {
    return ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .maxInboundMessageSize(50 * 1024 * 1024) // 50 MB
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(true)
        .maxRetryAttempts(3)
        .build()
}
```

### 2. Deadline/Timeout

Always set deadlines for your calls:

```kotlin
import io.grpc.Deadline
import java.util.concurrent.TimeUnit

val result = sessionStub
    .withDeadline(Deadline.after(5, TimeUnit.SECONDS))
    .openSession(command)
```

### 3. Interceptors

Add interceptors for logging and monitoring:

```kotlin
import io.grpc.*
import org.slf4j.LoggerFactory

class LoggingInterceptor : ClientInterceptor {

    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        logger.info("gRPC call: ${method.fullMethodName}")
        return next.newCall(method, callOptions)
    }
}

// Usage
@Bean
fun gameCoreChannel(): ManagedChannel {
    return ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .maxInboundMessageSize(50 * 1024 * 1024) // 50 MB
        .intercept(LoggingInterceptor())
        .build()
}
```

### 4. Health Checks

Implement health checks for the gRPC service:

```kotlin
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthGrpc
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class GameCoreHealthIndicator(
    private val channel: ManagedChannel
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val stub = HealthGrpc.newBlockingStub(channel)
            val response = stub.check(HealthCheckRequest.getDefaultInstance())
            Health.up().build()
        } catch (e: Exception) {
            Health.down(e).build()
        }
    }
}
```

### 5. TLS Configuration (Production)

For production environments, enable TLS:

```kotlin
@Bean
fun gameCoreChannel(): ManagedChannel {
    return ManagedChannelBuilder
        .forAddress(host, port)
        .useTransportSecurity()
        .maxInboundMessageSize(50 * 1024 * 1024) // 50 MB
        .build()
}
```

## Platform Types

Available platform types for session and game operations:

| Value | Description |
|-------|-------------|
| `PlatformType.DESKTOP` | Desktop browser |
| `PlatformType.MOBILE` | Mobile device |

## Support

For issues and questions, please refer to the main repository documentation or contact the development team.
