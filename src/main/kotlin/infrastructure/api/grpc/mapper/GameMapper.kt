package infrastructure.api.grpc.mapper

import domain.game.model.Game
import domain.game.model.GameWithDetails
import com.nekzabirov.igambling.proto.dto.GameDto
import com.nekzabirov.igambling.proto.service.FindGameResult

fun Game.toGameProto() = GameDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .setName(this.name)
    .setProviderId(this.providerId.toString())
    .setBonusBetEnable(this.bonusBetEnable)
    .setBonusWageringEnable(this.bonusWageringEnable)
    .setActive(this.active)
    .addAllTags(this.tags)
    .putAllImages(this.images.data)
    .build()

fun GameWithDetails.toFindGameResultProto() = FindGameResult.newBuilder()
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
    .setProvider(this.provider.toProviderProto())
    .setAggregator(this.aggregator.toAggregatorProto())
    .build()