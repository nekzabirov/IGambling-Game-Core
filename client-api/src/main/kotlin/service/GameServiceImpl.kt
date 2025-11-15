package service

import com.nekzabirov.igambling.proto.service.GameGrpcKt
import com.nekzabirov.igambling.proto.service.ListGameCommand
import com.nekzabirov.igambling.proto.service.ListGameResult
import core.value.Pageable
import io.ktor.server.application.*
import mapper.toCollectionProto
import mapper.toGameProto
import mapper.toGameVariantProto
import mapper.toPlatform
import mapper.toProviderProto
import org.koin.ktor.ext.get
import usecase.ListGameUsecase

class GameServiceImpl(application: Application) : GameGrpcKt.GameCoroutineImplBase() {
    private val listGameUsecase = application.get<ListGameUsecase>()

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
}