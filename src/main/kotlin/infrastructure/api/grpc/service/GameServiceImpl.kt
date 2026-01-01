package infrastructure.api.grpc.service

import application.port.outbound.MediaFile
import application.usecase.game.AddGameFavouriteUsecase
import application.usecase.game.AddGameTagUsecase
import application.usecase.game.DemoGameUsecase
import application.usecase.game.FindGameUsecase
import application.usecase.game.ListGamesUsecase
import application.usecase.game.RemoveGameFavouriteUsecase
import application.usecase.game.RemoveGameTagUsecase
import application.usecase.game.UpdateGameImageUsecase
import application.usecase.game.UpdateGameUsecase
import shared.value.Currency
import domain.common.value.Locale
import shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
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
import infrastructure.api.grpc.mapper.toCollectionProto
import infrastructure.api.grpc.mapper.toFindGameResultProto
import infrastructure.api.grpc.mapper.toGameProto
import infrastructure.api.grpc.mapper.toGameVariantProto
import infrastructure.api.grpc.mapper.toPlatform
import infrastructure.api.grpc.mapper.toProviderProto
import org.koin.ktor.ext.get

class GameServiceImpl(application: Application) : GameGrpcKt.GameCoroutineImplBase() {
    private val findGameUsecase = application.get<FindGameUsecase>()
    private val listGamesUsecase = application.get<ListGamesUsecase>()
    private val updateGameUsecase = application.get<UpdateGameUsecase>()
    private val updateGameImageUsecase = application.get<UpdateGameImageUsecase>()
    private val addGameTagUsecase = application.get<AddGameTagUsecase>()
    private val removeGameTagUsecase = application.get<RemoveGameTagUsecase>()
    private val addGameFavouriteUsecase = application.get<AddGameFavouriteUsecase>()
    private val removeGameFavouriteUsecase = application.get<RemoveGameFavouriteUsecase>()
    private val demoGameUsecase = application.get<DemoGameUsecase>()

    override suspend fun find(request: FindGameCommand): FindGameResult =
        findGameUsecase(identity = request.identity)
            .map { it.toFindGameResultProto() }
            .getOrElse { throw StatusException(Status.NOT_FOUND.withDescription(it.message)) }

    override suspend fun list(request: ListGameCommand): ListGameResult {
        val page = listGamesUsecase(pageable = Pageable(page = request.pageNumber, size = request.pageSize)) {
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
        }

        val games = page.items.map { item ->
            ListGameResult.Item.newBuilder()
                .setGame(item.game.toGameProto())
                .addAllCollectionIds(item.collections.map { c -> c.id.toString() })
                .setVariant(item.variant.toGameVariantProto())
                .build()
        }
        val providers = page.items.map { it.provider }.toSet().map { it.toProviderProto() }
        val collections = page.items.flatMap { it.collections }.toSet().map { it.toCollectionProto() }

        return ListGameResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllProviders(providers)
            .addAllCollections(collections)
            .addAllItems(games)
            .build()
    }

    override suspend fun update(request: UpdateGameConfig): EmptyResult =
        updateGameUsecase(
            identity = request.identity,
            active = request.active,
            bonusBet = request.bonusBet,
            bonusWagering = request.bonusWagering
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun updateImage(request: UpdateGameImageCommand): EmptyResult =
        updateGameImageUsecase(
            identity = request.identity,
            key = request.key,
            mediaFile = MediaFile(
                ext = request.ext,
                bytes = request.data.toByteArray()
            )
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun addTag(request: GameTagCommand): EmptyResult =
        addGameTagUsecase(identity = request.identity, tag = request.tag)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun removeTag(request: GameTagCommand): EmptyResult =
        removeGameTagUsecase(identity = request.identity, tag = request.tag)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun addFavourite(request: GameFavouriteCommand): EmptyResult =
        addGameFavouriteUsecase(gameIdentity = request.gameIdentity, playerId = request.playerId)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun removeFavourite(request: GameFavouriteCommand): EmptyResult =
        removeGameFavouriteUsecase(gameIdentity = request.gameIdentity, playerId = request.playerId)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun demoGame(request: DemoGameCommand): DemoGameResult =
        demoGameUsecase(
            gameIdentity = request.gameIdentity,
            currency = Currency(request.currency),
            locale = Locale(request.locale),
            platform = request.platform.toPlatform(),
            lobbyUrl = request.lobbyUrl
        )
            .map { DemoGameResult.newBuilder().setLaunchUrl(it.launchUrl).build() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}