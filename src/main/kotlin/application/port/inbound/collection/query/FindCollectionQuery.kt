package com.nekgamebling.application.port.inbound.collection.query

import application.port.inbound.Query
import domain.collection.model.Collection

data class FindCollectionQuery(val identity: String) : Query<FindCollectionResponse>

data class FindCollectionResponse(
    val collection: Collection,

    val providerCount: Int,

    val gameCount: Int
)
