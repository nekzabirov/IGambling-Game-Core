package app.usecase

import core.value.LocaleName
import domain.collection.model.Collection
import domain.collection.table.CollectionTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateCollectionUsecase {
    suspend operator fun invoke(identity: String, name: LocaleName, order: Int, active: Boolean): Result<Unit> = newSuspendedTransaction {
        CollectionTable.select(CollectionTable.id.count())
            .where { CollectionTable.identity eq identity }
            .count()
            .also {
                if (it <= 0) {
                    return@newSuspendedTransaction Result.failure(NotFoundException("Collection not found"))
                }
            }

        CollectionTable.update(where = { CollectionTable.identity eq identity }) {
            it[CollectionTable.name] = name
            it[CollectionTable.order] = order
            it[CollectionTable.active] = active
        }

        Result.success(Unit)
    }
}