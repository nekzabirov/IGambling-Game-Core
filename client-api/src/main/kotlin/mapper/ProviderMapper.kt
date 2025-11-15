package mapper

import com.nekzabirov.igambling.proto.dto.ProviderDto
import domain.provider.model.Provider

fun Provider.toProviderProto(): ProviderDto = ProviderDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .setName(this.name)
    .setOrder(this.order)
    .setAggregatorId(this.aggregatorId.toString())
    .setActive(this.active)
    .build()