package mapper

import com.nekzabirov.igambling.proto.dto.CollectionDto
import domain.collection.model.Collection

fun Collection.toCollectionProto() = CollectionDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .setActive(this.active)
    .putAllName(this.name.data)
    .putAllImages(this.images.data)
    .build()