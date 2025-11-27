package infrastructure.aggregator.onegamehub.adapter

import infrastructure.aggregator.onegamehub.dto.CreateFreespinDto
import infrastructure.aggregator.onegamehub.dto.GameUrlDto
import infrastructure.aggregator.onegamehub.model.OneGameHubConfig
import infrastructure.aggregator.onegamehub.model.OneGameHubPreset
import core.error.InvalidatePresetError
import domain.aggregator.adapter.IAggregatorAdapter
import domain.aggregator.adapter.IAggregatorPreset
import domain.aggregator.adapter.command.CancelFreespinCommand
import domain.aggregator.adapter.command.CreateFreenspinCommand
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import domain.aggregator.model.AggregatorGame
import domain.aggregator.model.Aggregator
import core.value.Locale
import core.model.Platform
import infrastructure.aggregator.onegamehub.dto.CancelFreespinDto
import infrastructure.aggregator.onegamehub.dto.GameDto
import infrastructure.aggregator.onegamehub.dto.ResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.*

class OneGameHubAdapter(override val config: OneGameHubConfig) : IAggregatorAdapter {
    companion object {
        private val SUPPORTED_LANGUAGES = listOf(
            "ab", "aa", "af", "ak", "sq", "am", "ar", "an", "hy", "as", "av", "ae", "ay", "az", "bm", "ba",
            "eu", "be", "bn", "bh", "bi", "bs", "br", "bg", "my", "ca", "ch", "ce", "ny", "zh", "cv", "kw",
            "co", "cr", "hr", "cs", "da", "dv", "nl", "dz", "en", "eo", "et", "ee", "fo", "fj", "fi", "fr",
            "ff", "gl", "ka", "de", "el", "gn", "gu", "ht", "ha", "he", "hz", "hi", "ho", "hu", "ia", "id",
            "ie", "ga", "ig", "ik", "io", "is", "it", "iu", "ja", "jv", "kl", "kn", "kr", "ks", "kk", "km",
            "ki", "rw", "ky", "kv", "kg", "ko", "ku", "kj", "la", "lb", "lg", "li", "ln", "lo", "lt", "lu",
            "lv", "gv", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mh", "mn", "na", "nv", "nd", "ne", "ng",
            "nb", "nn", "no", "ii", "nr", "oc", "oj", "cu", "om", "or", "os", "pa", "pi", "fa", "pl", "ps",
            "pt", "qu", "rm", "rn", "ro", "ru", "sa", "sc", "sd", "se", "sm", "sg", "sr", "gd", "sn", "si",
            "sk", "sl", "so", "st", "es", "su", "sw", "ss", "sv", "ta", "te", "tg", "th", "ti", "bo", "tk",
            "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa",
            "cy", "wo", "fy", "xh", "yi", "yo", "za", "zu"
        )
    }

    override val aggregator: Aggregator = Aggregator.ONEGAMEHUB

    private val addressUrl = "https://${config.gateway}/integrations/${config.partner}/rpc"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // 30 seconds
            connectTimeoutMillis = 10000  // 10 seconds
            socketTimeoutMillis = 30000   // 30 seconds
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    override suspend fun listGames(): Result<List<AggregatorGame>> {
        val response = client.get(addressUrl) {
            setAction("available_games")
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to fetch games from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<List<GameDto>>>()

        if (!responseBody.success) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))
        }

        val result = responseBody
            .response?.map {
                AggregatorGame(
                    symbol = it.id,
                    name = it.name,
                    providerName = it.brand,
                    aggregator = aggregator,
                    freeSpinEnable = it.freespinEnable,
                    freeChipEnable = false,
                    jackpotEnable = false,
                    demoEnable = it.demoEnable,
                    bonusBuyEnable = true,
                    locales = SUPPORTED_LANGUAGES.map { lang -> Locale(lang) },
                    platforms = listOf(Platform.DESKTOP, Platform.MOBILE)
                )
            }
            ?: emptyList()

        return Result.success(result)
    }

    override suspend fun getPreset(gameSymbol: String): Result<IAggregatorPreset> {
        val present = OneGameHubPreset().apply {
            quantity.default = 10
            quantity.minimal = 1

            betAmount.default = 100
            betAmount.minimal = 100

            lines.default = 10
            lines.minimal = 10
            lines.maximum = 10
        }

        return Result.success(present)
    }

    override suspend fun createFreespin(command: CreateFreenspinCommand): Result<Unit> {
        val preset = getPreset(gameSymbol = command.gameSymbol).getOrElse {
            return Result.failure(it)
        }.apply {
            pushValue(command.presetValue)
        } as OneGameHubPreset

        if (!preset.isValid()) {
            return Result.failure(InvalidatePresetError())
        }

        val amount = OneGameHubCurrencyAdapter.convertToAggregator(command.currency, preset.betAmount.value!!)

        val payload = CreateFreespinDto(
            id = command.referenceId,

            startAt = command.startAt,
            endAt = command.endAt,

            number = preset.quantity.value!!,

            playerId = command.playerId,

            currency = command.currency.value,

            gameId = command.gameSymbol,

            bet = amount,

            lineNumber = preset.lines.value!!
        )

        val response = client.post(addressUrl) {
            setAction("freerounds_create")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to create freespin from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<String>>()

        if (!responseBody.success) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))
        }

        return Result.success(Unit)
    }

    override suspend fun cancelFreespin(commad: CancelFreespinCommand): Result<Unit> {
        val payload = CancelFreespinDto(id = commad.referenceId)

        val response = client.post(addressUrl) {
            setAction("freerounds_cancel")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to cancel freespin from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<String>>()

        if (!responseBody.success) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))
        }

        return Result.success(Unit)
    }

    override suspend fun createLaunchUrl(command: CreateLaunchUrlCommand): Result<String> {
        val response = client.get(addressUrl) {
            setAction(if (command.isDemo) "demo_play" else "real_play")

            parameter("game_id", command.gameSymbol)

            if (!command.isDemo) {
                parameter("player_id", command.playerId)
            }

            parameter("currency", command.currency.value)
            parameter("mobile", if (command.platform == Platform.MOBILE) "1" else "0")
            parameter("language", command.locale.value)

            if (command.sessionToken.isNotBlank()) {
                parameter("extra", command.sessionToken)
            }

            parameter("return_url", command.lobbyUrl)
            parameter("deposit_url", command.lobbyUrl)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to create launch url from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<GameUrlDto>>()

        if (!responseBody.success || responseBody.response == null) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))

        }

        return Result.success(responseBody.response.gameUrl)
    }

    private fun HttpRequestBuilder.setAction(action: String) {
        parameter("action", action)
        parameter("secret", config.secret)
    }
}


