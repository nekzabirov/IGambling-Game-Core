package com.nekgamebling.infrastructure.api.grpc.mapper

import com.nekgamebling.domain.provider.model.Provider
import com.nekzabirov.igambling.proto.dto.ProviderDto

fun Provider.toProviderProto(): ProviderDto = ProviderDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .setName(this.name)
    .setOrder(this.order)
    .setAggregatorId(this.aggregatorId?.toString() ?: "")
    .setActive(this.active)
    .build()