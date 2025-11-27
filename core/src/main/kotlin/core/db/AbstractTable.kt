package core.db


import core.model.Page
import core.model.Pageable
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.time.LocalDateTime

abstract class AbstractTable(name: String) : UUIDTable(name) {
    val createdAt = datetime("created_at").default(LocalDateTime.now().toKotlinLocalDateTime())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now().toKotlinLocalDateTime())
}

fun Query.paging(pageable: Pageable): Page<ResultRow> {
    val totalCount = count()

    val items = limit(pageable.sizeReal).offset(pageable.offset)

    return Page(items = items.toList(), totalPages = pageable.getTotalPage(totalCount))
}