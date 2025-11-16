package app.usecase

import core.value.LocaleName
import domain.collection.model.Collection
import domain.collection.table.CollectionTable
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddCollectionUsecase {
    suspend operator fun invoke(identity: String, name: LocaleName): Result<Unit> = newSuspendedTransaction {
        CollectionTable.select(CollectionTable.id.count())
            .where { CollectionTable.identity eq identity }
            .count()
            .also {
                if (it > 0) {
                    return@newSuspendedTransaction Result.failure(BadRequestException("Identity already exists"))
                }
            }

        CollectionTable.insert {
            it[CollectionTable.identity] = identity
            it[CollectionTable.name] = name
        }

        Result.success(Unit)
    }
}