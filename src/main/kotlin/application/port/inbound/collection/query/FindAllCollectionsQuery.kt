package com.nekgamebling.application.port.inbound.collection.query

import application.port.inbound.Query
import domain.collection.model.Collection
import shared.value.Page
import shared.value.Pageable

data class FindAllCollectionsQuery(
    val pageable: Pageable,
    val query: String = "",
    val active: Boolean? = null
) : Query<FindAllCollectionsResponse>

data class FindAllCollectionsResponse(
    val result: Page<CollectionItem>
) {
    data class CollectionItem(
        val collection: Collection,
        val providerCount: Int,
        val gameCount: Int
    )
}
