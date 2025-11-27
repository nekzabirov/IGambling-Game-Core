import domain.aggregator.model.Aggregator
import domain.aggregator.table.AggregatorInfoTable
import app.usecase.SyncGameUsecase
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

class SyncGameTest : BaseTest() {
    private val syncGameUsecase = SyncGameUsecase()

    @Test
    fun syncGame() = doTest {
        val aggregatorIdentity = "onegamehub"

        withTimeout(5.minutes) {
            newSuspendedTransaction {
                AggregatorInfoTable.insert {
                    it[identity] = aggregatorIdentity
                    it[aggregator] = Aggregator.ONEGAMEHUB
                    it[config] = mapOf(
                        "gateway" to "staging.1gamehub.com",
                        "salt" to "c2e160ea-32f4-47f3-a3ff-6b8c5dbe9131",
                        "secret" to "02786922-6617-4333-aeda-c4863cf5ddb0",
                        "partner" to "moonbet-staging"
                    )
                }
            }

            val result = syncGameUsecase(aggregatorIdentity)

            assert(result.isSuccess)
        }
    }
}