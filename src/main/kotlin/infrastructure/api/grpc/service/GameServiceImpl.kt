package infrastructure.api.grpc.service

import application.port.inbound.command.*
import application.port.inbound.query.*
import application.port.outbound.MediaFile
import application.query.game.GameDetailsReadModel
import application.query.game.GameListReadModel
import application.service.GameService
import domain.game.repository.GameFilter
import infrastructure.handler.command.*
import infrastructure.handler.query.*
import shared.value.Currency
import domain.common.value.Locale
import shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.dto.GameDto
import com.nekzabirov.igambling.proto.dto.GameVariantDto
import com.nekzabirov.igambling.proto.dto.ProviderDto
import com.nekzabirov.igambling.proto.dto.CollectionDto
import com.nekzabirov.igambling.proto.service.DemoGameCommand
import com.nekzabirov.igambling.proto.service.DemoGameResult
import com.nekzabirov.igambling.proto.service.FindGameCommand
import com.nekzabirov.igambling.proto.service.FindGameResult
import com.nekzabirov.igambling.proto.service.GameFavouriteCommand
import com.nekzabirov.igambling.proto.service.GameGrpcKt
import com.nekzabirov.igambling.proto.service.GameTagCommand
import com.nekzabirov.igambling.proto.service.ListGameCommand
import com.nekzabirov.igambling.proto.service.ListGameResult
import com.nekzabirov.igambling.proto.service.UpdateGameConfig
import com.nekzabirov.igambling.proto.service.UpdateGameImageCommand
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import infrastructure.api.grpc.mapper.toPlatform
import infrastructure.api.grpc.mapper.toPlatformProto
import org.koin.ktor.ext.get

class GameServiceImpl(application: Application) : GameGrpcKt.GameCoroutineImplBase() {
    private val findGameQueryHandler = application.get<FindGameByIdentityQueryHandler>()
    private val listGamesQueryHandler = application.get<ListGamesQueryHandler>()
    private val updateGameCommandHandler = application.get<UpdateGameCommandHandler>()
    private val updateGameImageCommandHandler = application.get<UpdateGameImageCommandHandler>()
    private val addGameTagCommandHandler = application.get<AddGameTagCommandHandler>()
    private val removeGameTagCommandHandler = application.get<RemoveGameTagCommandHandler>()
    private val addGameFavouriteCommandHandler = application.get<AddGameFavouriteCommandHandler>()
    private val removeGameFavouriteCommandHandler = application.get<RemoveGameFavouriteCommandHandler>()
    private val gameService = application.get<GameService>()

    override suspend fun find(request: FindGameCommand): FindGameResult {
        val game = findGameQueryHandler.handle(FindGameByIdentityQuery(request.identity))
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Game not found: ${request.identity}"))
        return game.toFindGameResult()
    }

    override suspend fun list(request: ListGameCommand): ListGameResult {
        val filter = GameFilter.Builder().apply {
            query(request.query)

            if (request.hasActive()) {
                active(request.active)
            }

            if (request.hasBonusBet()) {
                bonusBet(request.bonusBet)
            }

            if (request.hasBonusWagering()) {
                bonusWagering(request.bonusWagering)
            }

            if (request.hasFreeSpinEnable()) {
                freeSpinEnable(request.freeSpinEnable)
            }

            if (request.hasFreeChipEnable()) {
                freeChipEnable(request.freeChipEnable)
            }

            if (request.hasJackpotEnable()) {
                jackpotEnable(request.jackpotEnable)
            }

            if (request.hasDemoEnable()) {
                demoEnable(request.demoEnable)
            }

            if (request.hasBonusBuyEnable()) {
                bonusBuyEnable(request.bonusBuyEnable)
            }

            request.platformsList.forEach { p ->
                platform(p.toPlatform())
            }

            request.providerIdentityList.forEach { provId ->
                providerIdentity(provId)
            }

            request.categoryIdentityList.forEach { collId ->
                collectionIdentity(collId)
            }

            request.tagsList.forEach { t ->
                tag(t)
            }

            if (request.hasPlayerId()) {
                playerId(request.playerId)
            }
        }.build()

        val page = listGamesQueryHandler.handle(
            ListGamesQuery(
                pageable = Pageable(page = request.pageNumber, size = request.pageSize),
                filter = filter
            )
        )

        val items = page.items.map { item -> item.toListGameItem() }

        // Get unique providers and collections from results
        val providers = page.items.map { item ->
            ProviderDto.newBuilder()
                .setIdentity(item.providerIdentity)
                .setName(item.providerName)
                .build()
        }.distinctBy { it.identity }

        return ListGameResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllProviders(providers)
            .addAllItems(items)
            .build()
    }

    override suspend fun update(request: UpdateGameConfig): EmptyResult =
        updateGameCommandHandler.handle(
            UpdateGameCommand(
                identity = request.identity,
                active = request.active,
                bonusBetEnable = request.bonusBet,
                bonusWageringEnable = request.bonusWagering
            )
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun updateImage(request: UpdateGameImageCommand): EmptyResult =
        updateGameImageCommandHandler.handle(
            application.port.inbound.command.UpdateGameImageCommand(
                identity = request.identity,
                key = request.key,
                mediaFile = MediaFile(
                    ext = request.ext,
                    bytes = request.data.toByteArray()
                )
            )
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun addTag(request: GameTagCommand): EmptyResult =
        addGameTagCommandHandler.handle(AddGameTagCommand(identity = request.identity, tag = request.tag))
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun removeTag(request: GameTagCommand): EmptyResult =
        removeGameTagCommandHandler.handle(RemoveGameTagCommand(identity = request.identity, tag = request.tag))
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun addFavourite(request: GameFavouriteCommand): EmptyResult =
        addGameFavouriteCommandHandler.handle(
            AddGameFavouriteCommand(gameIdentity = request.gameIdentity, playerId = request.playerId)
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun removeFavourite(request: GameFavouriteCommand): EmptyResult =
        removeGameFavouriteCommandHandler.handle(
            RemoveGameFavouriteCommand(gameIdentity = request.gameIdentity, playerId = request.playerId)
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun demoGame(request: DemoGameCommand): DemoGameResult =
        gameService.launchDemo(
            gameIdentity = request.gameIdentity,
            currency = Currency(request.currency),
            locale = Locale(request.locale),
            platform = request.platform.toPlatform(),
            lobbyUrl = request.lobbyUrl
        )
            .map { DemoGameResult.newBuilder().setLaunchUrl(it.launchUrl).build() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    // Extension functions to convert read models to proto
    private fun GameDetailsReadModel.toFindGameResult(): FindGameResult = FindGameResult.newBuilder()
        .setId(this.id.toString())
        .setIdentity(this.identity)
        .setName(this.name)
        .putAllImages(this.images.data)
        .setBonusBetEnable(this.bonusBetEnable)
        .setBonusWageringEnable(this.bonusWageringEnable)
        .addAllTags(this.tags)
        .setSymbol(this.symbol)
        .setFreeSpinEnable(this.freeSpinEnable)
        .setFreeChipEnable(this.freeChipEnable)
        .setJackpotEnable(this.jackpotEnable)
        .setDemoEnable(this.demoEnable)
        .setBonusBuyEnable(this.bonusBuyEnable)
        .addAllLocales(this.locales.map { it.value })
        .addAllPlatforms(this.platforms.map { it.toPlatformProto() })
        .setPlayLines(this.playLines)
        .setProvider(ProviderDto.newBuilder()
            .setIdentity(this.providerIdentity)
            .setName(this.providerName)
            .build())
        .build()

    private fun GameListReadModel.toListGameItem(): ListGameResult.Item = ListGameResult.Item.newBuilder()
        .setGame(GameDto.newBuilder()
            .setId(this.id.toString())
            .setIdentity(this.identity)
            .setName(this.name)
            .putAllImages(this.images.data)
            .setActive(this.active)
            .addAllTags(this.tags)
            .build())
        .addAllCollectionIds(this.collectionIdentities)
        .setVariant(GameVariantDto.newBuilder()
            .setSymbol(this.symbol)
            .setFreeSpinEnable(this.freeSpinEnable)
            .setFreeChipEnable(this.freeChipEnable)
            .setJackpotEnable(this.jackpotEnable)
            .setDemoEnable(this.demoEnable)
            .setBonusBuyEnable(this.bonusBuyEnable)
            .addAllPlatforms(this.platforms.map { it.toPlatformProto() })
            .build())
        .build()
}
