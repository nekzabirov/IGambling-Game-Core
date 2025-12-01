package com.nekgamebling.infrastructure.api.grpc.mapper

import com.nekgamebling.domain.game.model.Game
import com.nekzabirov.igambling.proto.dto.GameDto

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