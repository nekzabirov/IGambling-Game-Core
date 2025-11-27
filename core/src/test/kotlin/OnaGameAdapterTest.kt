import infrastructure.aggregator.onegamehub.adapter.OneGameHubAdapter
import infrastructure.aggregator.onegamehub.model.OneGameHubConfig
import infrastructure.aggregator.onegamehub.model.OneGameHubPreset
import domain.aggregator.adapter.command.CreateFreenspinCommand
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import core.value.Currency
import core.value.Locale
import core.model.Platform
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.isNotEmpty
import kotlin.test.Test

class OnaGameAdapterTest : BaseTest() {
    private val config = OneGameHubConfig().apply {
        parse(mapOf(
            "gateway" to "staging.1gamehub.com",
            "salt" to "c2e160ea-32f4-47f3-a3ff-6b8c5dbe9131",
            "secret" to "02786922-6617-4333-aeda-c4863cf5ddb0",
            "partner" to "moonbet-staging"
        ))
    }

    private val adapter = OneGameHubAdapter(config)

    @Test
    fun listGames() = doTest {
        val result = adapter.listGames()

        assert(result.isSuccess)

        assert(result.getOrThrow().isNotEmpty())
    }

    @Test
    fun createFreespin() = doTest {
        val preset = OneGameHubPreset(quantity = 10, betAmount = 100, lines = 10)

        val command = CreateFreenspinCommand(
            referenceId = UUID.randomUUID().toString(),
            playerId = UUID.randomUUID().toString(),
            gameSymbol = "pragmatic-play-african-elephant",
            currency = Currency("EUR"),
            startAt = LocalDateTime.now().toKotlinLocalDateTime(),
            endAt = LocalDateTime.now().plusMinutes(10).toKotlinLocalDateTime(),
            preset = preset
        )

        val result = adapter.createFreespin(command)

        assert(result.isSuccess)
    }

    @Test
    fun createLaunchUrl() = doTest {
        val command = CreateLaunchUrlCommand(
            gameSymbol = "pragmatic-play-african-elephant",
            playerId = UUID.randomUUID().toString(),
            sessionToken = "908092390283592350",
            lobbyUrl = "https:://google.com",
            locale = Locale("en"),
            currency = Currency("EUR"),
            platform = Platform.DESKTOP,
            isDemo = false
        )

        val result = adapter.createLaunchUrl(command)

        assert(result.isSuccess)
    }
}