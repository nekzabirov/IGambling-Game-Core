package service

import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.DemoGameCommand
import com.nekzabirov.igambling.proto.service.DemoGameResult
import com.nekzabirov.igambling.proto.service.GameFavouriteCommand
import com.nekzabirov.igambling.proto.service.GameGrpcKt
import com.nekzabirov.igambling.proto.service.GameTagCommand
import com.nekzabirov.igambling.proto.service.ListGameCommand
import com.nekzabirov.igambling.proto.service.ListGameResult
import com.nekzabirov.igambling.proto.service.UpdateGameConfig
import core.value.Currency
import core.value.Locale
import core.model.Pageable
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import mapper.toCollectionProto
import mapper.toGameProto
import mapper.toGameVariantProto
import mapper.toPlatform
import mapper.toProviderProto
import org.koin.ktor.ext.get
import app.usecase.AddGameFavouriteUsecase
import app.usecase.AddGameTagUsecase
import app.usecase.DemoGameUsecase
import app.usecase.ListGameUsecase
import app.usecase.RemoveGameFavouriteUsecase
import app.usecase.RemoveGameTagUsecase
import app.usecase.UpdateGameUsecase

class GameServiceImpl(application: Application) : GameGrpcKt.GameCoroutineImplBase() {
    private val listGameUsecase = application.get<ListGameUsecase>()
    private val updateGameUsecase = application.get<UpdateGameUsecase>()
    private val addGameTagUsecase = application.get<AddGameTagUsecase>()
    private val removeGameTagUsecase = application.get<RemoveGameTagUsecase>()
    private val addGameFavouriteUsecase = application.get<AddGameFavouriteUsecase>()
    private val removeGameFavouriteUsecase = application.get<RemoveGameFavouriteUsecase>()
    private val demoGameUsecase = application.get<DemoGameUsecase>()

    override suspend fun list(request: ListGameCommand): ListGameResult =
        listGameUsecase(pageable = Pageable(page = request.pageNumber, size = request.pageSize)) {
            withQuery(request.query)

            if (request.hasActive()) {
                withActive(request.active)
            }

            if (request.hasBonusBet()) {
                withBonusBet(request.bonusBet)
            }

            if (request.hasBonusWagering()) {
                withBonusWagering(request.bonusWagering)
            }

            if (request.hasFreeSpinEnable()) {
                withFreeSpinEnable(request.freeSpinEnable)
            }

            if (request.hasFreeChipEnable()) {
                withFreeChipEnable(request.freeChipEnable)
            }

            if (request.hasJackpotEnable()) {
                withJackpotEnable(request.jackpotEnable)
            }

            if (request.hasDemoEnable()) {
                withDemoEnable(request.demoEnable)
            }

            if (request.hasBonusBuyEnable()) {
                withBonusBuyEnable(request.bonusBuyEnable)
            }

            request.platformsList.forEach { platform ->
                withPlatform(platform.toPlatform())
            }

            request.providerIdentityList.forEach { providerIdentity ->
                withProviderIdentity(providerIdentity)
            }

            request.categoryIdentityList.forEach { categoryIdentity ->
                withCategoryIdentity(categoryIdentity)
            }

            request.tagsList.forEach { tag ->
                withTag(tag)
            }

            if (request.hasPlayerId()) {
                withPlayer(request.playerId)
            }
        }
            .let { page ->
                val games = page.items.map {
                    ListGameResult.Item.newBuilder()
                        .setGame(it.game.toGameProto())
                        .addAllCollectionIds(it.categories.map { c -> c.id.toString() })
                        .setVariant(it.variant.toGameVariantProto())
                        .build()
                }
                val providers = page.items.map { it.provider }.toSet().map { it.toProviderProto() }
                val collections = page.items.flatMap { it.categories }.toSet().map { it.toCollectionProto() }

                ListGameResult.newBuilder()
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